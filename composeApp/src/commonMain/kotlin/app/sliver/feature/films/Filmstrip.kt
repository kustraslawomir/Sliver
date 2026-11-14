package app.sliver.feature.films

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.feature.trim.rememberPhoto

/** Real thumbnails across the film (up to 15) with a white playhead; tap or drag to seek. */
@Composable
fun Filmstrip(plan: FilmPlan, fraction: Float, onSeek: (Float) -> Unit) {
    val picks = remember(plan) {
        val n = plan.days.size
        if (n == 0) emptyList() else List(minOf(15, n)) { i -> plan.days[(i * n / minOf(15, n)).coerceAtMost(n - 1)] }
    }
    val seek by androidx.compose.runtime.rememberUpdatedState(onSeek)
    Box(
        Modifier.fillMaxWidth().height(36.dp).clip(SliverShapes.R12).background(SliverColors.Surface2)
            .pointerInput(Unit) { detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> seek((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            picks.forEach { d ->
                val img = rememberPhoto(d.thumb)
                Box(Modifier.weight(1f).fillMaxHeight().background(d.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3)) {
                    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val x = (fraction.coerceIn(0f, 1f) * size.width).coerceIn(1.5.dp.toPx(), size.width - 1.5.dp.toPx())
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx())
        }
    }
}

/** "Apr · May · Jun" — the months the film spans, at most 8. */
fun stripMonths(plan: FilmPlan): String {
    val months = plan.days.map { it.date.year to it.date.month.ordinal }.distinct()
    val pick = if (months.size <= 8) months else List(8) { months[it * (months.size - 1) / 7] }
    val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return pick.joinToString(" \u00B7 ") { names[it.second] }
}
