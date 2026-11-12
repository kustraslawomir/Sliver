package app.sliver.feature.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.data.db.Day
import app.sliver.feature.trim.rememberPhoto
import kotlin.math.abs
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

private val Weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Brightness 0..1 of a clip; clips saved before the column existed fall back to their thumbnail colour until backfilled. */
fun lightOf(day: Day): Float = if (day.luma >= 0f) day.luma else {
    val c = day.color
    (0.2126f * (c shr 16 and 0xFF) + 0.7152f * (c shr 8 and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
}

fun toneOf(day: Day): Int = if (day.tone != 0) day.tone else day.color

/** Index (0 until [span]) of the bar under a tap at [x] of [width]. */
fun barIndexAt(x: Float, width: Float, span: Int): Int = (x / width * span).toInt().coerceIn(0, span - 1)

/** The clip day closest to [index] within [radius] bars, so thin bars stay tappable. */
fun nearestClipDay(index: Int, first: LocalDate, radius: Int, hasClip: (LocalDate) -> Boolean): LocalDate? =
    (0..radius).flatMap { d -> if (d == 0) listOf(index) else listOf(index - d, index + d) }
        .map { first.plus(DatePeriod(days = it)) }.firstOrNull(hasClip)

/** Brightest clip day of the month containing [date], among [days]. */
fun brightestOfMonth(days: Collection<Day>, date: LocalDate): LocalDate? =
    days.filter { it.date.year == date.year && it.date.month == date.month }.maxByOrNull { lightOf(it) }?.date

@Composable
internal fun LightOfYear(
    byDate: Map<LocalDate, Day>,
    today: LocalDate,
    first: LocalDate,
    onMonth: (YearMonth) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val span = (first.daysUntil(today) + 1).coerceAtLeast(1)
    var sel by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = sel?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.takeIf { it in byDate }
        ?: byDate.keys.filter { it <= today }.maxOrNull()
    val stub = SliverColors.LineStrong
    val ember = SliverColors.AccentMark
    val ink = SliverColors.TextPrimary
    val density = LocalDensity.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SliverText("The light of your year", SliverType.Headline.copy(fontSize = 14.sp, lineHeight = 20.sp))
        Canvas(
            Modifier.fillMaxWidth().height(190.dp)
                .semantics { contentDescription = "The light of your year. One bar per day, taller is brighter. Tap a bar." }
                .pointerInput(span, first, byDate) {
                    detectTapGestures { p ->
                        val idx = barIndexAt(p.x, size.width.toFloat(), span)
                        nearestClipDay(idx, first, 2) { it in byDate }
                            ?.let { sel = it.toString() }
                    }
                },
        ) {
            val step = size.width / span
            val barW = (step * 0.78f).coerceAtLeast(1.dp.toPx())
            val base = size.height - 6.dp.toPx()
            val maxH = 172.dp.toPx()
            val radius = CornerRadius(1.dp.toPx())
            for (i in 0 until span) {
                val date = first.plus(DatePeriod(days = i))
                if (date >= today && date !in byDate) continue
                val x = i * step
                val day = byDate[date]
                if (day == null) {
                    drawRoundRect(stub, Offset(x, base - 2.dp.toPx()), Size(barW, 2.dp.toPx()), radius)
                } else {
                    val h = (maxH * lightOf(day)).coerceAtLeast(4.dp.toPx())
                    val c = toneOf(day).let { if (it == 0) SliverColors.TextTertiary else Color(it) }
                    drawRoundRect(c.copy(alpha = if (date == selected) 1f else 0.9f), Offset(x, base - h), Size(barW, h), radius)
                }
            }
            selected?.let {
                val x = first.daysUntil(it) * step + barW / 2
                drawRect(ink, Offset(x - 0.75.dp.toPx(), 2.dp.toPx()), Size(1.5.dp.toPx(), size.height - 2.dp.toPx()))
            }
            if (today !in byDate) drawCircle(ember, 3.dp.toPx(), Offset((span - 1) * step + barW / 2, base))
        }
        BoxWithMonthLabels(first, today, span, onMonth)
        SliverText("Taller = brighter clip. Colour = the clip itself. Tap a bar.", SliverType.Caption.copy(fontSize = 12.sp, lineHeight = 17.sp))
    }
    selected?.let { date ->
        val day = byDate.getValue(date)
        val brightest = remember(byDate, date) { brightestOfMonth(byDate.values, date) }
        val thumb = rememberPhoto(day.thumb)
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenDay(date) },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(140.dp, 79.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(Color(toneOf(day)))) {
                if (thumb != null) Image(thumb, null, Modifier.size(140.dp, 79.dp), contentScale = ContentScale.Crop)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SliverText("${Weekdays[date.dayOfWeek.ordinal]} ${date.day} ${MonthNames[date.month.ordinal]}", SliverType.Headline.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp))
                SliverText(
                    if (brightest == date) "The brightest day of ${MonthLong[date.month.ordinal]}" else "Day ${first.daysUntil(date) + 1} of this project",
                    SliverType.Caption.copy(fontSize = 13.sp, lineHeight = 18.sp),
                )
            }
        }
    }
}

@Composable
private fun BoxWithMonthLabels(first: LocalDate, today: LocalDate, span: Int, onMonth: (YearMonth) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp)) {
        val step = maxWidth / span
        var lastX = (-100).dp
        var ym = first.toYearMonth()
        val end = today.toYearMonth()
        while (ym <= end) {
            val start = LocalDate(ym.year, ym.month, 1)
            val x = step * first.daysUntil(maxOf(start, first))
            if (x - lastX >= 30.dp) {
                lastX = x
                val target = ym
                SliverText(
                    MonthNames[ym.month - 1], SliverType.tnum(12.sp, 0.sp, SliverColors.TextTertiary),
                    Modifier.offset(x = x).height(32.dp).clickable(role = Role.Button) { onMonth(target) }
                        .semantics { contentDescription = MonthLong[target.month - 1] },
                )
            }
            ym = if (ym.month == 12) YearMonth(ym.year + 1, 1) else YearMonth(ym.year, ym.month + 1)
        }
    }
}
