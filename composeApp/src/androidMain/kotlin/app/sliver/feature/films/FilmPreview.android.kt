package app.sliver.feature.films

import kotlinx.coroutines.flow.first
import app.sliver.data.look.LookRandom
import app.sliver.core.theme.SliverColors
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import app.sliver.core.ui.rememberAppVisible
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams
import app.sliver.data.look.LookEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIX_ORIGINAL = 0.5f
private const val MUSIC_SYNC_SLACK_MS = 250L

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
actual fun FilmPreview(
    clips: List<FilmClip>,
    look: LookParams?,
    audio: FilmAudio,
    track: String?,
    paused: Boolean,
    seek: FilmSeek?,
    onPosition: (index: Int, clipFraction: Float) -> Unit,
    modifier: Modifier,
    musicVolume: Int?,
    compare: Boolean,
) {
    if (LocalInspectionMode.current) {
        Box(modifier.background(SliverColors.Surface3))
        return
    }
    val context = LocalContext.current
    // Owner (Samsung, 116-clip film): switching filters left the preview black with the clock stuck. Swapping the effect
    // chain on a live player does not recover on long playlists, while effects set before the surface always work. So
    // every filter gets a fresh player built with its effects; clip, position and pause state carry over.
    var effects by remember { mutableStateOf<List<androidx.media3.common.Effect>?>(null) }
    val seed = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    LaunchedEffect(look) {
        effects = withContext(Dispatchers.IO) {
            look?.let { LookEffects.build(context, it, seed::get, forExport = false, animate = true) }.orEmpty()
                .ifEmpty { listOf(androidx.media3.effect.Contrast(0f)) }
        }
    }
    val resume = remember { longArrayOf(0L, 0L) }
    val video = effects?.let { fx ->
        remember(fx) {
            app.sliver.core.media.newPlayer(context).apply {
                setVideoEffects(fx)
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
    }
    val music = remember { app.sliver.core.media.newPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ALL } }
    DisposableEffect(Unit) { onDispose { music.release() } }
    // Hold to compare: one clip, no filter, at the moment the film is paused on (the filtered player keeps its playlist).
    val clean = remember {
        app.sliver.core.media.newPlayer(context).apply { setVideoEffects(listOf(androidx.media3.effect.Contrast(0f))); volume = 0f }
    }
    DisposableEffect(Unit) { onDispose { clean.release() } }
    val comparing = compare && look != null
    DisposableEffect(video) {
        onDispose {
            video?.let {
                resume[0] = it.currentMediaItemIndex.toLong()
                resume[1] = it.currentPosition
                it.release()
            }
        }
    }
    val report = rememberUpdatedState(onPosition)
    val currentClips = rememberUpdatedState(clips)
    val currentAudio = rememberUpdatedState(audio)
    val currentVideo = rememberUpdatedState(video)
    val pausedNow = rememberUpdatedState(paused)
    val frameShown = remember { kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    // With effects on, a paused player has no frame after prepare or seek; a short play renders it, then it pauses again.
    suspend fun showFrameWhilePaused(player: ExoPlayer) {
        if (!pausedNow.value) return
        player.playWhenReady = true
        kotlinx.coroutines.withTimeoutOrNull(600) { frameShown.first() }
        delay(60)
        if (pausedNow.value) player.playWhenReady = false
    }
    DisposableEffect(video, clips) {
        val player = video ?: return@DisposableEffect onDispose {}
        fun update() { clips.getOrNull(player.currentMediaItemIndex)?.let { seed.set(LookRandom.seedOf(it.path)) } }
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = update()
            override fun onRenderedFirstFrame() { frameShown.tryEmit(Unit) }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(video, clips) {
        val player = video ?: return@LaunchedEffect
        val index = resume[0].toInt().takeIf { it in clips.indices } ?: 0
        player.setMediaItems(clips.map { MediaItem.fromUri(it.path) }, index, resume[1])
        clips.getOrNull(index)?.let { seed.set(LookRandom.seedOf(it.path)) }
        player.prepare()
        showFrameWhilePaused(player)
    }
    LaunchedEffect(track, audio) {
        if (track != null && audio != FilmAudio.ORIGINAL) {
            music.setMediaItem(MediaItem.fromUri(trackUri(track)))
            music.prepare()
        } else {
            music.clearMediaItems()
        }
    }
    // Same music level as the render (owner: adjustable background music volume).
    LaunchedEffect(audio, FilmPlayback.muted, musicVolume) {
        music.volume = if (FilmPlayback.muted) 0f else audioPlan(audio, track, musicVolume).musicVolume.takeIf { it > 0f } ?: 1f
    }
    val visible = rememberAppVisible()
    LaunchedEffect(comparing) {
        if (!comparing) { clean.stop(); clean.clearMediaItems(); return@LaunchedEffect }
        val player = video ?: return@LaunchedEffect
        val clip = clips.getOrNull(player.currentMediaItemIndex) ?: return@LaunchedEffect
        clean.setMediaItem(MediaItem.fromUri(clip.path), player.currentPosition)
        clean.prepare()
        clean.playWhenReady = true
        val shown = kotlinx.coroutines.CompletableDeferred<Unit>()
        val l = object : Player.Listener { override fun onRenderedFirstFrame() { shown.complete(Unit) } }
        clean.addListener(l)
        kotlinx.coroutines.withTimeoutOrNull(800) { shown.await() }
        delay(40)
        clean.removeListener(l)
        clean.playWhenReady = false
    }
    LaunchedEffect(comparing, video) {
        val player = video ?: return@LaunchedEffect
        if (!comparing) showFrameWhilePaused(player)
    }
    LaunchedEffect(video, paused, visible, comparing) {
        val play = !paused && visible && !comparing
        video?.let {
            if (play && it.playbackState == Player.STATE_IDLE) it.prepare()
            it.playWhenReady = play
        }
        music.playWhenReady = play
    }
    LaunchedEffect(seek) {
        val s = seek ?: return@LaunchedEffect
        val n = clips.size
        if (n == 0) return@LaunchedEffect
        val index = dayIndexAt(s.fraction, n)
        val clipFraction = s.fraction.coerceIn(0f, 1f) * n - index
        val player = video ?: return@LaunchedEffect
        val duration = player.duration.takeIf { it > 0 } ?: 1000L
        player.seekTo(index, (clipFraction * duration).toLong())
        showFrameWhilePaused(player)
    }
    LaunchedEffect(Unit) {
        while (true) {
            val video = currentVideo.value
            if (video == null) { delay(33); continue }
            val index = video.currentMediaItemIndex
            val clip = currentClips.value.getOrNull(index)
            video.volume = when {
                clip == null -> 0f
                FilmPlayback.muted -> 0f
                currentAudio.value == FilmAudio.MUSIC -> 0f
                !clip.keepAudio -> 0f
                currentAudio.value == FilmAudio.MIX -> MIX_ORIGINAL
                else -> 1f
            }
            val duration = video.duration.takeIf { it > 0 } ?: 1000L
            // Owner: the music must follow the film when seeking (and when the film loops), as it does in the render.
            val musicMs = music.duration
            if (music.mediaItemCount > 0 && musicMs > 0) {
                val want = musicPositionAt(index, video.currentPosition, musicMs)
                if (musicDrift(music.currentPosition, want, musicMs) > MUSIC_SYNC_SLACK_MS) music.seekTo(want)
            }
            report.value(index, (video.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            delay(33)
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        update = { val p = if (comparing) clean else video; if (it.player !== p) it.player = p },
        modifier = modifier,
    )
}

@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { onPicked(withContext(Dispatchers.IO) { importAudio(context, uri) }) }
    }
    return { launcher.launch(arrayOf("audio/*")) }
}

@Composable
actual fun rememberMusicLibrary(refresh: Any?): List<String> {
    val context = LocalContext.current
    return remember(refresh) {
        java.io.File(context.filesDir, "music").listFiles().orEmpty().filter { it.isFile && it.length() > 0 }
            .sortedByDescending { it.lastModified() }.map { it.path }
    }
}
