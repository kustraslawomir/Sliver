package app.sliver.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

/** Same look as the Android shadow layer: only the blurred shadow is drawn, never the shape itself. */
actual fun Modifier.glow(shape: Shape, vararg layers: GlowLayer, stroke: Dp?): Modifier = drawBehind {
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
            is Outline.Rectangle -> addRect(o.rect)
            is Outline.Rounded -> addRoundRect(o.roundRect)
            is Outline.Generic -> addPath(o.path)
        }
    }.asSkiaPath()
    drawIntoCanvas { canvas ->
        layers.forEach { l ->
            // Android blur radius r maps to Gaussian sigma ≈ 0.57735 r + 0.5.
            val sigma = l.blur.toPx() * 0.9f * 0.57735f + 0.5f
            val paint = Paint().apply {
                isAntiAlias = true
                if (stroke != null) {
                    mode = PaintMode.STROKE
                    strokeWidth = stroke.toPx()
                }
                imageFilter = ImageFilter.makeDropShadowOnly(0f, l.dy.toPx(), sigma, sigma, l.color.toArgb(), null, null)
            }
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}
