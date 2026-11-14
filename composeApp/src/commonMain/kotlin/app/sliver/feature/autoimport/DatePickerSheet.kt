package app.sliver.feature.autoimport

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.monthGrid
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.feature.archive.MonthLong
import app.sliver.feature.archive.canGoNext
import app.sliver.feature.archive.canGoPrev
import app.sliver.feature.archive.toYearMonth
import app.sliver.feature.gallery.formatDay
import kotlinx.datetime.LocalDate

private val Weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Audit v1.8 §7 date picker (board phone 2): title + month arrows, "October 2026 · days after today can't be picked",
 * the app's month grid (44 dp day circles, selected = paper fill, out of range dimmed and disabled), quick chips and a
 * primary button that names the result. Picking a day selects it; the button confirms.
 */
@Composable
fun DatePickerSheet(
    visible: Boolean, selected: LocalDate, min: LocalDate, max: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit,
    title: String = "Pick a day",
    chips: List<Pair<String, LocalDate>> = emptyList(),
    confirm: (LocalDate) -> String = { "Use ${it.day} ${MonthLong[it.month.ordinal]}" },
) {
    SliverBottomSheet(visible, onDismiss) {
        var picked by remember(selected, visible) { mutableStateOf(selected) }
        var shown by remember(selected, visible) { mutableStateOf(selected.toYearMonth()) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SliverText(title, SliverType.Title, Modifier.weight(1f))
                IconButton44({ if (canGoPrev(shown, min)) shown = shown.plusMonths(-1) }, "Previous month") {
                    SliverIcon(SliverIcons.ChevronLeft, if (canGoPrev(shown, min)) SliverColors.TextPrimary else SliverColors.TextDisabled, size = 20.dp)
                }
                IconButton44({ if (canGoNext(shown, max)) shown = shown.plusMonths(1) }, "Next month") {
                    SliverIcon(SliverIcons.ChevronRight, if (canGoNext(shown, max)) SliverColors.TextPrimary else SliverColors.TextDisabled, size = 20.dp)
                }
            }
            SliverText("${MonthLong[shown.month - 1]} ${shown.year} \u00B7 days after today can't be picked", SliverType.Caption, color = SliverColors.TextTertiary)
            Row(Modifier.fillMaxWidth()) {
                Weekdays.forEach {
                    SliverText(it, SliverType.Caption.copy(textAlign = TextAlign.Center), Modifier.weight(1f), SliverColors.TextTertiary)
                }
            }
            // Owner: same height for every month, so the sheet never jumps: always 6 week rows.
            val weeks = monthGrid(shown.year, shown.month).filter { week -> week.any { it != null } }
            (weeks + List(6 - weeks.size) { List<LocalDate?>(7) { null } }).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(46.dp), Alignment.Center) {
                            if (date != null) DateCell(date, date == picked, date in min..max) { picked = date }
                        }
                    }
                }
            }
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.filter { it.second in min..max }.forEach { (label, date) ->
                        app.sliver.core.kit.ReelogChip(label, picked == date, { picked = date; shown = date.toYearMonth() })
                    }
                }
            }
            app.sliver.core.ui.PrimaryButton(confirm(picked), { onPick(picked) }, Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@Composable
private fun DateCell(date: LocalDate, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .then(if (selected) Modifier.background(SliverColors.TextPrimary) else Modifier)
            .semantics { this.selected = selected; contentDescription = formatDay(date) }
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        Alignment.Center,
    ) {
        SliverText(
            date.day.toString(), SliverType.Num.copy(textAlign = TextAlign.Center),
            color = when {
                !enabled -> SliverColors.CellFuture
                selected -> SliverColors.BgBase
                else -> SliverColors.TextPrimary
            },
        )
    }
}
