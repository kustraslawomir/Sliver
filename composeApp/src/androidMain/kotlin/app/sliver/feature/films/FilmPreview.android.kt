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

private const val MUSIC_SYNC_SLACK_MS = 250L

private fun layerKey(i: Int, path: String) = "$i|$path"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
actual fun FilmPreview(
    clips: List<FilmClip>,
    look: LookParams?,
    mix: AudioMix,
    paused: Boolean,
    seek: FilmSeek?,
    onPosition: (index: Int, clipFraction: Float) -> Unit,
    modifier: Modifier,
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
    // One player per audio layer; each follows the film's time (see layerPositionAt).
    val layerPlayers = remember { mutableMapOf<String, ExoPlayer>() }
    DisposableEffect(Unit) { onDispose { layerPlayers.values.forEach { it.release() }; layerPlayers.clear() } }
    val currentMix = rememberUpdatedState(mix)
    val playing = remember { mutableStateOf(false) }
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
    // The same file can sit on two layers, so players are keyed by lane and file.
    val layerKeys = mix.layers.mapIndexed { i, l -> layerKey(i, l.path) }.filter { trackExists(it.substringAfter('|')) }
    LaunchedEffect(layerKeys) {
        (layerPlayers.keys - layerKeys.toSet()).forEach { layerPlayers.remove(it)?.release() }
        layerKeys.filter { it !in layerPlayers }.forEach { key ->
            layerPlayers[key] = app.sliver.core.media.newPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(trackUri(key.substringAfter('|'))))
                prepare()
            }
        }
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
        playing.value = play
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
            val mixNow = currentMix.value
            video.volume = if (clip == null || FilmPlayback.muted) 0f else mixNow.clipGain(clip.date, clip.keepAudio)
            val duration = video.duration.takeIf { it > 0 } ?: 1000L
            // Owner: audio must follow the film when seeking (and when the film loops), as it does in the render.
            val filmMs = index * FILM_FRAME_MS + video.currentPosition
            mixNow.layers.forEachIndexed { i, layer ->
                val p = layerPlayers[layerKey(i, layer.path)] ?: return@forEachIndexed
                val want = layerPositionAt(layer, filmMs)?.takeIf { !layer.muted }
                if (want == null) { p.playWhenReady = false; return@forEachIndexed }
                p.volume = if (FilmPlayback.muted) 0f else layer.volume / 100f
                if (kotlin.math.abs(p.currentPosition - want) > MUSIC_SYNC_SLACK_MS) p.seekTo(want)
                p.playWhenReady = playing.value
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
