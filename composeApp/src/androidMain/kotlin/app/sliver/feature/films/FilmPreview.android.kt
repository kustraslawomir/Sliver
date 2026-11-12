package app.sliver.feature.films

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
private const val MIX_MUSIC = 0.7f

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
) {
    if (LocalInspectionMode.current) {
        Box(modifier.background(SliverColors.Surface3))
        return
    }
    val context = LocalContext.current
    val video = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    val music = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    DisposableEffect(Unit) { onDispose { video.release(); music.release() } }
    val report = rememberUpdatedState(onPosition)
    val currentClips = rememberUpdatedState(clips)
    val currentAudio = rememberUpdatedState(audio)

    LaunchedEffect(clips) {
        video.setMediaItems(clips.map { MediaItem.fromUri(it.path) })
        video.prepare()
    }
    LaunchedEffect(clips, look, paused) {
        val params = look ?: clips.getOrNull(video.currentMediaItemIndex)?.look ?: LookParams()
        video.setVideoEffects(LookEffects.build(context, params, forExport = false, animateGrain = !paused))
    }
    DisposableEffect(video, clips, look) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (look != null) return
                val params = clips.getOrNull(video.currentMediaItemIndex)?.look ?: return
                video.setVideoEffects(LookEffects.build(context, params, forExport = false))
            }
        }
        video.addListener(listener)
        onDispose { video.removeListener(listener) }
    }
    LaunchedEffect(track, audio) {
        if (track != null && audio != FilmAudio.ORIGINAL) {
            music.setMediaItem(MediaItem.fromUri(trackUri(track)))
            music.prepare()
        } else {
            music.clearMediaItems()
        }
    }
    LaunchedEffect(audio) { music.volume = if (audio == FilmAudio.MIX) MIX_MUSIC else 1f }
    val visible = rememberAppVisible()
    LaunchedEffect(paused, visible) {
        video.playWhenReady = !paused && visible
        music.playWhenReady = !paused && visible
    }
    LaunchedEffect(seek) {
        val s = seek ?: return@LaunchedEffect
        val n = clips.size
        if (n == 0) return@LaunchedEffect
        val index = dayIndexAt(s.fraction, n)
        val clipFraction = s.fraction.coerceIn(0f, 1f) * n - index
        val duration = video.duration.takeIf { it > 0 } ?: 1000L
        video.seekTo(index, (clipFraction * duration).toLong())
    }
    LaunchedEffect(Unit) {
        while (true) {
            val index = video.currentMediaItemIndex
            val clip = currentClips.value.getOrNull(index)
            video.volume = when {
                clip == null -> 0f
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
