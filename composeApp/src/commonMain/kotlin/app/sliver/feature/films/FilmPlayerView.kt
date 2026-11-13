package app.sliver.feature.films

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Jump request in milliseconds; a new instance seeks even when the position is unchanged. */
class PlayerSeek(val ms: Long)

/** Plays the rendered film file. Reports position and total duration (0 until known); [onEnded] fires at the end. */
@Composable
expect fun FilmPlayerView(
    path: String,
    playing: Boolean,
    muted: Boolean,
    seek: PlayerSeek?,
    onPosition: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier,
)

/** Switches the screen to landscape with hidden system bars while [on]; restores portrait afterwards. */
@Composable
expect fun FullscreenLandscape(on: Boolean, rotate: Boolean)

/** Owner: mute and pause carry over between the film player and the builder (Edit film). */
object FilmPlayback {
    var muted by androidx.compose.runtime.mutableStateOf(false)
    var paused by androidx.compose.runtime.mutableStateOf(false)
}
