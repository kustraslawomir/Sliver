package app.sliver.feature.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.sliver.core.theme.SliverColors
import app.sliver.core.ui.rememberAppVisible
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
    val player = remember { app.sliver.core.media.newPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ONE } }
    // A second player stays prepared on the full file so a drag starts instantly and seeks to key frames (fast).
    val scrubPlayer = remember(path) {
        app.sliver.core.media.newPlayer(context).apply {
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            playWhenReady = false
            volume = 0f
            setMediaItem(MediaItem.fromUri(path))
            prepare()
        }
    }
    DisposableEffect(scrubPlayer) { onDispose { scrubPlayer.release() } }
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
        scrubPlayer.addListener(listener)
        onDispose {
            player.removeListener(listener);
            scrubPlayer.removeListener(listener);
            player.release()
        }
    }

    val scrubbing = scrubMs != null
    val view = remember { arrayOfNulls<PlayerView>(1) }
    LaunchedEffect(scrubbing) {
        if (scrubbing) {
            player.playWhenReady = false
            scrubPlayer.seekTo(scrubMs ?: startMs)
            view[0]?.player = scrubPlayer
        } else {
            view[0]?.player = player
        }
    }
    LaunchedEffect(scrubMs) { scrubMs?.let { scrubPlayer.seekTo(it) } }

    LaunchedEffect(path, startMs, windowMs, scrubbing) {
        if (scrubbing) return@LaunchedEffect
        val clip = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(startMs + windowMs)
            .build()
        player.setMediaItem(MediaItem.Builder().setUri(path).setClippingConfiguration(clip).build())
        player.prepare()
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
                view[0] = this
            }
        },
        modifier = modifier,
        onRelease = { it.player = null },
    )
}
