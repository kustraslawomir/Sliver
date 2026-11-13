package app.sliver.feature.autoimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.db.Day

/** All gallery items of one auto-filled or draft day: pick one to cut, keep the current pick, or remove the day. */
@Composable
fun DayMediaSheetBody(day: Day, busy: Boolean, onPick: (String) -> Unit, onKeep: () -> Unit, onRemove: () -> Unit) {
    val candidates = remember(day.draftCandidates) { decodeCandidates(day.draftCandidates) }
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SliverText(dayItemsTitle(day.date, candidates.size.coerceAtLeast(1)), SliverType.Title)
        if (candidates.isNotEmpty()) {
            SliverText("Tap an item to choose it and cut the clip.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                candidates.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        row.forEach { c ->
                            CandidateTile(c, c.uri == candidates.first().uri, Modifier.weight(1f).aspectRatio(1f)) { onPick(c.uri) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        PrimaryButton("Keep", onKeep, Modifier.fillMaxWidth(), enabled = !busy)
        GhostButton("Remove day", onRemove, Modifier.fillMaxWidth(), destructive = true, enabled = !busy)
    }
}

/** The header chip's sheet: drafts and auto-filled days waiting for a look. */
@Composable
fun ReviewSheetBody(drafts: Int, auto: Int, busy: Boolean, onKeepAll: () -> Unit, onRemoveUnchecked: () -> Unit, onWalk: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SliverText(reviewChipLabel(drafts, auto), SliverType.Title)
        SliverText(
            "Keep them as they are, go through them one by one, or remove the auto-filled days you didn't check.",
            SliverType.BodySmall, color = SliverColors.TextSecondary,
        )
        PrimaryButton("Keep all ${drafts + auto}", onKeepAll, Modifier.fillMaxWidth(), enabled = !busy)
        SecondaryButton("Check one by one", onWalk, Modifier.fillMaxWidth())
        if (auto > 0) GhostButton("Remove unchecked", onRemoveUnchecked, Modifier.fillMaxWidth(), destructive = true, enabled = !busy)
    }
}
