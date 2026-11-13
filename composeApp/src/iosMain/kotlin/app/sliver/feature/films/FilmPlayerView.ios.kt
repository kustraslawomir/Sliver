package app.sliver.feature.films

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// TODO iOS later: AVPlayerLayer.
@Composable
actual fun FilmPlayerView(
    path: String,
    playing: Boolean,
    muted: Boolean,
    seek: PlayerSeek?,
    onPosition: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier,
) = Box(modifier.background(Color.Black))

@Composable
actual fun FullscreenLandscape(on: Boolean, rotate: Boolean) = Unit
