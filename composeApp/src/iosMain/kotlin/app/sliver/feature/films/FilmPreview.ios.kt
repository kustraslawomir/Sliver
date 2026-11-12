package app.sliver.feature.films

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.sliver.core.log.logDebug
import app.sliver.data.FilmAudio
import app.sliver.data.LookParams

// TODO iOS later: AVQueuePlayer preview and UIDocumentPicker for audio.
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
) = Box(modifier.background(Color.Black))

@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio) -> Unit): () -> Unit = { logDebug("AudioPicker", "not on iOS yet") }

// TODO iOS later: Core Image / Metal port of the look shader for filter thumbnails.
actual suspend fun renderFilterThumbs(
    frame: androidx.compose.ui.graphics.ImageBitmap,
    presets: List<app.sliver.data.LookPreset>,
): Map<app.sliver.data.LookPreset, androidx.compose.ui.graphics.ImageBitmap> = emptyMap()
