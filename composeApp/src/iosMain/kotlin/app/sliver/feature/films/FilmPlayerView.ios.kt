package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FilmPlayerView(
    path: String,
    playing: Boolean,
    muted: Boolean,
    seek: PlayerSeek?,
    onPosition: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier,
) = TODO("iOS later")

@Composable
actual fun FullscreenLandscape(on: Boolean) = TODO("iOS later")
