package app.sliver.core.designsystem

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

actual fun Modifier.glow(shape: Shape, vararg layers: GlowLayer, stroke: Dp?): Modifier = drawBehind {
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
            is Outline.Rectangle -> addRect(o.rect)
            is Outline.Rounded -> addRoundRect(o.roundRect)
            is Outline.Generic -> addPath(o.path)
        }
    }.asAndroidPath()
    drawIntoCanvas { canvas ->
        layers.forEach { l ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (stroke != null) {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke.toPx()
                } else style = Paint.Style.FILL
                // Paint is transparent: only the shadow layer is visible, so the element itself is not painted over.
                color = l.color.copy(alpha = 0.001f).toArgb()
                setShadowLayer(l.blur.toPx() * 0.9f, 0f, l.dy.toPx(), l.color.toArgb())
            }
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}
