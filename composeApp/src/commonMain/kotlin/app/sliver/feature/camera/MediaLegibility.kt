package app.sliver.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import kotlin.math.pow

const val MEDIA_PLATE_ALPHA = 0.6f

/** Soft dark shadow for text drawn straight over the live preview. */
fun TextStyle.overMedia(): TextStyle = copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), 4f))

/** Small dark pill behind text that sits over the live preview. */
fun Modifier.mediaPlate(): Modifier =
    clip(SliverShapes.Pill).background(SliverColors.Media.copy(alpha = MEDIA_PLATE_ALPHA)).padding(horizontal = 8.dp, vertical = 2.dp)

private fun channel(v: Float): Float = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun luminance(r: Float, g: Float, b: Float): Float = 0.2126f * channel(r) + 0.7152f * channel(g) + 0.0722f * channel(b)

/** WCAG contrast of [fg] over a plate of [plate] at [plateAlpha] laid on a background of [bg]. */
fun plateContrast(fg: Color, plate: Color, plateAlpha: Float, bg: Color): Float {
    fun mix(p: Float, b: Float) = p * plateAlpha + b * (1f - plateAlpha)
    val l1 = luminance(fg.red, fg.green, fg.blue)
    val l2 = luminance(mix(plate.red, bg.red), mix(plate.green, bg.green), mix(plate.blue, bg.blue))
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}
