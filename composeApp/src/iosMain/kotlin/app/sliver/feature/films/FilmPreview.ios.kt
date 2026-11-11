package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams

@Composable
actual fun FilmPreview(
    clips: List<FilmClip>,
    look: LookParams?,
    audio: FilmAudio,
    track: String?,
    paused: Boolean,
    seek: FilmSeek?,
    onPosition: (index: Int, clipFraction: Float) -> Unit,
    modifier: Modifier,
): Unit = TODO("iOS later")

@Composable
actual fun rememberAudioPicker(onPicked: (String) -> Unit): () -> Unit = TODO("iOS later")
