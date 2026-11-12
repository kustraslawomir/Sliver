package app.sliver.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.data.StampStyle
import app.sliver.data.export.LED_DIGIT_DP
import app.sliver.data.export.LED_ORANGE
import app.sliver.data.export.STAMP_MARGIN_DP
import app.sliver.data.export.SevenSegment

/**
 * Date stamp in a corner of the enclosing box. PLAIN: white mono, bottom-right. LED: orange segmented digits, bottom-right.
 * CAMCORDER: white mono caps, bottom-left.
 */
@Composable
fun BoxScope.DateStamp(text: String, modifier: Modifier = Modifier, style: StampStyle = StampStyle.PLAIN) {
    if (style == StampStyle.LED) {
        val density = LocalDensity.current.density
        val h = LED_DIGIT_DP * density
        val w = SevenSegment.width(text, h)
        val rects = SevenSegment.rects(text, h)
        with(LocalDensity.current) {
            Canvas(modifier.align(Alignment.BottomEnd).padding(STAMP_MARGIN_DP.dp).size(w.toDp(), h.toDp())) {
                rects.forEach { r ->
                    drawRect(Color(LED_ORANGE).copy(alpha = 0.25f), Offset(r.x - 1.5f, r.y - 1.5f), Size(r.w + 3f, r.h + 3f))
                    drawRect(Color(LED_ORANGE), Offset(r.x, r.y), Size(r.w, r.h))
                }
            }
        }
        return
    }
    SliverText(
        text,
        SliverType.Stamp.copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 4f * LocalDensity.current.density)),
        modifier.align(if (style == StampStyle.CAMCORDER) Alignment.BottomStart else Alignment.BottomEnd).padding(STAMP_MARGIN_DP.dp),
        Color.White,
    )
}
