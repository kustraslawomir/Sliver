package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams

/** [date] (ISO) keys the film's per-clip mute. */
class FilmClip(val path: String, val keepAudio: Boolean, val date: String = "")

/** Jump request; a new instance seeks even when the fraction is unchanged. */
class FilmSeek(val fraction: Float)

/**
 * Plays [clips] back to back (one second each). [look] is the film's filter (null = clean); clips never carry their own.
 * [mix] is the film's sound: clip volume, muted clips and audio layers, kept in step with the film's time.
 * [onPosition] reports the current clip index and progress through it.
 */
@Composable
expect fun FilmPreview(
    clips: List<FilmClip>,
    look: LookParams?,
    mix: AudioMix,
    paused: Boolean,
    seek: FilmSeek?,
    onPosition: (index: Int, clipFraction: Float) -> Unit,
    modifier: Modifier,
    /** Owner: while held, the preview shows the same moment without the filter (paused) to compare. */
    compare: Boolean = false,
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

/** Length of an audio file in ms (0 when unreadable). Blocking; small files only read their header. */
expect fun audioDurationMs(path: String): Long
