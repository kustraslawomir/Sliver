package app.sliver.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors

data class GlowLayer(val color: Color, val blur: Dp, val dy: Dp = 0.dp)

/**
 * Soft light behind the element, like CSS box-shadow without spread.
 * CSS "0 0 14px #FF8A3D88" is GlowLayer(Ember.copy(alpha = 0x88 / 255f), 14.dp).
 * Pass the shape the element is clipped to; [stroke] for outline-only elements.
 */
expect fun Modifier.glow(shape: Shape, vararg layers: GlowLayer, stroke: Dp? = null): Modifier

private fun ember(alpha: Int) = SliverColors.Accent.copy(alpha = alpha / 255f)

/** Tokens from design/tokens.json "glow". No glow in the light theme. */
object Glow {
    private val none = emptyArray<GlowLayer>()
    private fun on(vararg layers: GlowLayer) = if (SliverColors.light) none else arrayOf(*layers)

    val cta get() = on(GlowLayer(ember(0x44), 32.dp, dy = 8.dp))
    val record get() = on(GlowLayer(ember(0x4D), 12.dp))
    val recordBreath get() = on(GlowLayer(ember(0x73), 18.dp))
    val photoRecord = arrayOf(GlowLayer(Color.White.copy(alpha = 0x33 / 255f), 16.dp))
    val frame get() = on(GlowLayer(ember(0x99), 14.dp), GlowLayer(ember(0x55), 36.dp))
    val slit get() = on(GlowLayer(ember(0xFF), 12.dp), GlowLayer(ember(0x88), 32.dp))
    val slitHero get() = on(*slit, GlowLayer(ember(0x88), 110.dp))
    val dot get() = on(GlowLayer(ember(0xFF), 8.dp))
    val lock get() = on(GlowLayer(ember(0x99), 10.dp))
    val stampShadowAlpha = 0x99 / 255f
}
