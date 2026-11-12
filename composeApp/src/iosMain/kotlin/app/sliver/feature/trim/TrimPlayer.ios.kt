package app.sliver.feature.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// TODO iOS later: AVPlayerLayer looping the trim window.
@Composable
actual fun TrimPlayer(
    path: String,
    startMs: Long,
    windowMs: Long,
    speed: Float,
    audio: Boolean,
    paused: Boolean,
    modifier: Modifier,
    scrubMs: Long?,
    onAspect: (Float) -> Unit,
) = Box(modifier.background(Color.Black))
