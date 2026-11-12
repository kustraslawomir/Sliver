package app.sliver.feature.trim

import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import app.sliver.core.theme.SliverColors
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import app.sliver.core.ui.rememberAppVisible
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
    scrubMs: Long?,
    onAspect: (Float) -> Unit,
) {
    if (LocalInspectionMode.current) {
        // Screenshot tests have no decoder: show the still instead of a playing video.
        val still = rememberPhoto(path)
        LaunchedEffect(path) { onAspect(9f / 16f) }
        if (still != null) Image(still, null, modifier, contentScale = ContentScale.Crop) else Box(modifier.background(SliverColors.Surface3))
        return
    }
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

    val scrubbing = scrubMs != null
    LaunchedEffect(path, scrubbing) {
        if (!scrubbing) return@LaunchedEffect
        player.playWhenReady = false
        player.setMediaItem(MediaItem.fromUri(path))
        player.prepare()
    }
    LaunchedEffect(scrubMs) { scrubMs?.let { player.seekTo(it) } }

    LaunchedEffect(path, startMs, windowMs, scrubbing) {
        if (scrubbing) return@LaunchedEffect
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
    val visible = rememberAppVisible()
    LaunchedEffect(paused, visible) { player.playWhenReady = !paused && visible }

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
