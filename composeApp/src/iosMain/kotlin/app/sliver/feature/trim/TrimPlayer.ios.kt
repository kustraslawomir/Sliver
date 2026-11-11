package app.sliver.feature.trim

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun TrimPlayer(
    path: String,
    startMs: Long,
    windowMs: Long,
    speed: Float,
    audio: Boolean,
    paused: Boolean,
    modifier: Modifier,
    look: app.sliver.data.LookParams,
): Unit = TODO("iOS later")
