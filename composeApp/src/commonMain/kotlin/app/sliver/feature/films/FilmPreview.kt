package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams

class FilmClip(val path: String, val keepAudio: Boolean)

/** Jump request; a new instance seeks even when the fraction is unchanged. */
class FilmSeek(val fraction: Float)

/**
 * Plays [clips] back to back (one second each). [look] is the film's filter (null = clean); clips never carry their own.
 * [track] is a path to a picked music file, played looped according to [audio].
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
    musicVolume: Int? = null,
)

/** Result of picking a music file: a copy in app-private storage, or a user-facing reason it could not be used. */
sealed interface PickedAudio {
    class Ok(val path: String) : PickedAudio
    class Failed(val message: String) : PickedAudio
}

/** Opens the system document picker for an audio file; the file is copied into app storage and checked to decode. */
@Composable
expect fun rememberAudioPicker(onPicked: (PickedAudio) -> Unit): () -> Unit

/** Owner: music files loaded once stay in the app; newest first. [refresh] re-reads the list after an import. */
@Composable
expect fun rememberMusicLibrary(refresh: Any?): List<String>
