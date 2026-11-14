package app.sliver.feature.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverText
import app.sliver.core.time.monthGrid
import app.sliver.data.db.Day
import kotlinx.datetime.LocalDate

/**
 * HubV17/V18 Year: the 12 months of [year] in 3 columns, one small square per day — filled days in their clip colour,
 * empty past days dim, future days faint. Tapping a month opens it in the Month view.
 */
@Composable
fun YearView(year: Int, byDate: Map<LocalDate, Day>, today: LocalDate, start: LocalDate, onMonth: (YearMonth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        (1..12).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { m ->
                    val ym = YearMonth(year, m)
                    val filled = remember(byDate, year, m) {
                        byDate.values.count { it.date.year == year && it.date.month.ordinal + 1 == m && !it.draft }
                    }
                    val open = LocalDate(year, m, 1).let { it <= today && YearMonth(start.year, start.month.ordinal + 1) <= ym }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = open, role = Role.Button) { onMonth(ym) }
                            .semantics { contentDescription = "${MonthLong[m - 1]}, $filled clips" }
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SliverText(
                            MonthLong[m - 1].take(3),
                            ReelogType.label,
                            color = if (open) SliverColors.TextPrimary else SliverColors.TextDisabled,
                        )
                        MiniMonth(year, m, byDate, today, start)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(year: Int, month: Int, byDate: Map<LocalDate, Day>, today: LocalDate, start: LocalDate) {
    val weeks = remember(year, month) { monthGrid(year, month).filter { w -> w.any { it != null } } }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    val day = date?.let { byDate[it] }?.takeIf { !it.draft }
                    val color = when {
                        date == null -> Color.Transparent
                        day != null -> day.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.TextSecondary
                        date > today || date < start -> SliverColors.Surface1.copy(alpha = 0.5f)
                        else -> SliverColors.Surface2
                    }
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(color))
                }
            }
        }
    }
}
