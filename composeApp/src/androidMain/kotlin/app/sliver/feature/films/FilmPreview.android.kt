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
) {
    if (LocalInspectionMode.current) {
        Box(modifier.background(SliverColors.Surface3))
        return
    }
    val context = LocalContext.current
    // Effects must be on the player before PlayerView gives it a surface, otherwise frames bypass the effect pipeline
    // (owner: "filters are not on the video preview"). Start with an identity effect and swap the list later.
    val video = remember {
        app.sliver.core.media.newPlayer(context).apply {
            setVideoEffects(listOf(androidx.media3.effect.Contrast(0f)))
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
    val music = remember { app.sliver.core.media.newPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ALL } }
    DisposableEffect(Unit) { onDispose { video.release(); music.release() } }
    val report = rememberUpdatedState(onPosition)
    val currentClips = rememberUpdatedState(clips)
    val currentAudio = rememberUpdatedState(audio)

    LaunchedEffect(clips) {
        video.setMediaItems(clips.map { MediaItem.fromUri(it.path) })
        video.prepare()
    }
    // Rebuilding the effect chain on every 1 s clip stalled playback; build it once per filter and let the
    // shader read the current clip's seed each frame (same seed as the export, so preview = export).
    val seed = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    DisposableEffect(video, clips) {
        fun update() { clips.getOrNull(video.currentMediaItemIndex)?.let { seed.set(LookRandom.seedOf(it.path)) } }
        update()
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = update()
        }
        video.addListener(listener)
        onDispose { video.removeListener(listener) }
    }
    // Owner: pause → drag the seekbar → black frame that resume could not fix. With effects on, a paused seek leaves the
    // video sink without a frame; a short play renders the frame at the new position, then it pauses again.
    val pausedNow = rememberUpdatedState(paused)
    val frameShown = remember { kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    DisposableEffect(video) {
        val l = object : Player.Listener {
            override fun onRenderedFirstFrame() { frameShown.tryEmit(Unit) }
        }
        video.addListener(l)
        onDispose { video.removeListener(l) }
    }
    suspend fun showFrameWhilePaused() {
        if (!pausedNow.value) return
        video.playWhenReady = true
        kotlinx.coroutines.withTimeoutOrNull(400) { frameShown.first() }
        delay(60)
        if (pausedNow.value) video.playWhenReady = false
    }
    // Owner: pause → change filter → resume left the video black and the clock stuck. Rebuilding on pause and switching
    // between "no effects" and "effects" re-created the GL pipeline under a paused player; now the chain is rebuilt only
    // when the filter changes, it is never empty (identity effect for no filter), and the frame is redrawn after a swap.
    LaunchedEffect(look) {
        val effects = withContext(Dispatchers.IO) {
            look?.let { LookEffects.build(context, it, seed::get, forExport = false, animate = true) }.orEmpty()
                .ifEmpty { listOf(androidx.media3.effect.Contrast(0f)) }
        }
        video.setVideoEffects(effects)
        video.seekTo(video.currentMediaItemIndex, video.currentPosition)
        showFrameWhilePaused()
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
    LaunchedEffect(paused, visible) {
        val play = !paused && visible
        if (play && video.playbackState == Player.STATE_IDLE) video.prepare()
        video.playWhenReady = play
        music.playWhenReady = play
    }
    LaunchedEffect(seek) {
        val s = seek ?: return@LaunchedEffect
        val n = clips.size
        if (n == 0) return@LaunchedEffect
        val index = dayIndexAt(s.fraction, n)
        val clipFraction = s.fraction.coerceIn(0f, 1f) * n - index
        val duration = video.duration.takeIf { it > 0 } ?: 1000L
        video.seekTo(index, (clipFraction * duration).toLong())
        showFrameWhilePaused()
    }
    LaunchedEffect(Unit) {
        while (true) {
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
            report.value(index, (video.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            delay(33)
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = video
            }
        },
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
