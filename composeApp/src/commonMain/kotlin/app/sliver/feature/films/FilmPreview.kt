package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams

class FilmClip(val path: String, val look: LookParams, val keepAudio: Boolean)

/** Jump request; a new instance seeks even when the fraction is unchanged. */
class FilmSeek(val fraction: Float)

/**
 * Plays [clips] back to back (one second each). [look] overrides every clip's own look when set.
 * [track] is an "asset:" reference or a content URI, played looped according to [audio].
 * [onPosition] reports the current clip index and progress through it.
 */
@Composable
expect fun FilmPreview(
    clips: List<FilmClip>,
    look: LookParams?,
    audio: FilmAudio,
    track: String?,
    paused: Boolean,
    seek: FilmSeek?,
    onPosition: (index: Int, clipFraction: Float) -> Unit,
    modifier: Modifier,
)

/** Opens the system file picker for an audio file and returns a persistent reference to it. */
@Composable
expect fun rememberAudioPicker(onPicked: (String) -> Unit): () -> Unit
