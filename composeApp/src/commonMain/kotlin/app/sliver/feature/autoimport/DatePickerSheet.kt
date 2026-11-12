package app.sliver.feature.autoimport

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

/** Month grid in a bottom sheet; days after [max] and before [min] are disabled. Tokens only, no stock picker look. */
@Composable
fun DatePickerSheet(visible: Boolean, selected: LocalDate, min: LocalDate, max: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    SliverBottomSheet(visible, onDismiss) {
        var shown by remember(selected) { mutableStateOf(selected.toYearMonth()) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton44({ if (canGoPrev(shown, min)) shown = shown.plusMonths(-1) }, "Previous month") {
                    SliverIcon(SliverIcons.ChevronLeft, if (canGoPrev(shown, min)) SliverColors.TextPrimary else SliverColors.TextDisabled)
                }
                SliverText("${MonthLong[shown.month - 1]} ${shown.year}", SliverType.Headline)
                IconButton44({ if (canGoNext(shown, max)) shown = shown.plusMonths(1) }, "Next month") {
                    SliverIcon(SliverIcons.ChevronRight, if (canGoNext(shown, max)) SliverColors.TextPrimary else SliverColors.TextDisabled)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Weekdays.forEach {
                    SliverText(it, SliverType.Caption.copy(textAlign = TextAlign.Center), Modifier.weight(1f), SliverColors.TextTertiary)
                }
            }
            monthGrid(shown.year, shown.month).filter { week -> week.any { it != null } }.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(44.dp), Alignment.Center) {
                            if (date != null) DateCell(date, date == selected, date in min..max) { onPick(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(date: LocalDate, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(40.dp).fillMaxWidth()
            .clip(SliverShapes.S)
            .then(if (selected) Modifier.background(SliverColors.AccentTint).border(1.dp, SliverColors.AccentLine, SliverShapes.S) else Modifier)
            .semantics { this.selected = selected; contentDescription = formatDay(date) }
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        Alignment.Center,
    ) {
        SliverText(
            date.day.toString(), SliverType.Num.copy(textAlign = TextAlign.Center),
            color = when {
                !enabled -> SliverColors.TextDisabled
                selected -> SliverColors.AccentText
                else -> SliverColors.TextPrimary
            },
        )
    }
}
