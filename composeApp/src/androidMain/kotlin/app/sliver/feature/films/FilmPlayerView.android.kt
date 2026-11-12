package app.sliver.feature.films

import app.sliver.core.ui.rememberAppVisible
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
actual fun FilmPlayerView(
    path: String,
    playing: Boolean,
    muted: Boolean,
    seek: PlayerSeek?,
    onPosition: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    val report = rememberUpdatedState(onPosition)
    val ended = rememberUpdatedState(onEnded)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) ended.value()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(path) {
        player.setMediaItem(MediaItem.fromUri(path))
        player.prepare()
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    val visible = rememberAppVisible()
    LaunchedEffect(playing, visible) {
        if (playing && visible && player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.playWhenReady = playing && visible
    }
    LaunchedEffect(seek) { seek?.let { player.seekTo(it.ms) } }
    LaunchedEffect(Unit) {
        while (true) {
            report.value(player.currentPosition, player.duration.takeIf { it > 0 } ?: 0L)
            delay(33)
        }
    }
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

@Composable
actual fun FullscreenLandscape(on: Boolean) {
    val activity = LocalContext.current.activity() ?: return
    DisposableEffect(on) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (on) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
