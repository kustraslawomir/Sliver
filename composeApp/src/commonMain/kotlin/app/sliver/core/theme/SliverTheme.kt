package app.sliver.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun SliverTheme(reducedMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion, content = content)
}

@Composable
fun SliverText(
    text: String,
    style: TextStyle = SliverType.Body,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier, if (color == Color.Unspecified) style else style.copy(color = color), maxLines = maxLines)
}

/** Neutral sheet shadow only; ember glow lives in core/designsystem/Glow.kt. */
object SliverGlow {
    fun sheet(shape: Shape): Modifier = Modifier.shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
}
