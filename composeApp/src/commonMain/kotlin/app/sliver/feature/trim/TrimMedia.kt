package app.sliver.feature.trim

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.LookParams

interface TrimMedia {
    suspend fun durationMs(path: String): Long
    /** Displayed width / height of the clip (rotation applied); 1 when unknown. */
    suspend fun aspect(path: String): Float
    suspend fun autoPickMs(path: String, durationMs: Long): Long
    suspend fun thumbnails(path: String, durationMs: Long, count: Int): List<ImageBitmap>
}

/**
 * Looping preview of [windowMs] of [path] starting at [startMs], played at [speed]; [onAspect] reports the display width / height. [windowMs] is the source span
 * (1000 * speed, or the whole clip when short); [paused] freezes it while the strip is dragged.
 */
@Composable
expect fun TrimPlayer(
    path: String,
    startMs: Long,
    windowMs: Long,
    speed: Float,
    audio: Boolean,
    paused: Boolean,
    modifier: Modifier,
    look: LookParams = LookParams(),
    onAspect: (Float) -> Unit = {},
)
