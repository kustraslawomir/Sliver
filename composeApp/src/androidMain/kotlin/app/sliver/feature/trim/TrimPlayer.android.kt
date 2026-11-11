package app.sliver.feature.trim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.sliver.data.LookParams
import app.sliver.data.look.LookEffects
import app.sliver.feature.camera.displaySize

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
actual fun TrimPlayer(
    path: String,
    startMs: Long,
    windowMs: Long,
    speed: Float,
    audio: Boolean,
    paused: Boolean,
    modifier: Modifier,
    look: LookParams,
    onAspect: (Float) -> Unit,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }
    val reportAspect = rememberUpdatedState(onAspect)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    val (w, h) = displaySize(size.width, size.height, size.unappliedRotationDegrees)
                    reportAspect.value(w * size.pixelWidthHeightRatio / h)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(path, startMs, windowMs) {
        val clip = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(startMs + windowMs)
            .build()
        player.setMediaItem(MediaItem.Builder().setUri(path).setClippingConfiguration(clip).build())
        player.prepare()
    }
    LaunchedEffect(look, paused) {
        player.setVideoEffects(LookEffects.build(context, look, forExport = false, animateGrain = !paused))
    }
    LaunchedEffect(speed) { player.playbackParameters = PlaybackParameters(speed) }
    LaunchedEffect(audio) { player.volume = if (audio) 1f else 0f }
    LaunchedEffect(paused) { player.playWhenReady = !paused }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
        modifier = modifier,
    )
}
