package app.sliver.core.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.sliver.core.theme.SliverColors
import androidx.compose.ui.unit.dp

enum class GlassStyle { Regular, Strong }

/** The lit edge that makes it read as glass: 1 dp, white 30 % at the top → 6 % at the bottom. */
internal fun glassEdge(light: Boolean) =
    if (light) Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.7f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f)))

/** Solid fallback (API < 31, battery saver): #1C1C1F at 92 % (light: white 92 %), same edge, no blur. */
fun Modifier.glass(style: GlassStyle, shape: Shape): Modifier {
    val light = SliverColors.light
    val fill = if (light) Color.White.copy(alpha = 0.92f) else Color(0xFF1C1C1F).copy(alpha = if (style == GlassStyle.Strong) 0.94f else 0.92f)
    return clip(shape).background(fill, shape).border(1.dp, glassEdge(light), shape)
}
