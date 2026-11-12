package app.sliver.feature.trim

import app.sliver.core.theme.SliverColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Centers content of the given display [aspect] on black: 16:9 fills the width, 9:16 fills the height. */
@Composable
fun ClipFrame(aspect: Float?, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(SliverColors.Media), Alignment.Center) {
        Box(if (aspect != null && aspect > 0f) Modifier.aspectRatio(aspect) else Modifier.fillMaxSize(), content = content)
    }
}

/** [TrimPlayer] that sizes itself to the clip's real display aspect; [overlay] sits exactly over the picture. */
@Composable
fun ClipPlayer(
    path: String,
    startMs: Long,
    windowMs: Long,
    modifier: Modifier,
    speed: Float = 1f,
    audio: Boolean = false,
    paused: Boolean = false,
    scrubMs: Long? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    var aspect by remember(path) { mutableStateOf<Float?>(null) }
    ClipFrame(aspect, modifier) {
        TrimPlayer(path, startMs, windowMs, speed, audio, paused, Modifier.fillMaxSize(), scrubMs) { aspect = it }
        overlay()
    }
}
