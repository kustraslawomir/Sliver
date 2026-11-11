package app.sliver.feature.backfill

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.Notices
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SwitchRow
import app.sliver.data.DaySource
import app.sliver.data.repo.DayRepository
import app.sliver.feature.archive.MonthNames
import app.sliver.feature.archive.YearMonth
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.rememberMultiMediaPicker
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@Composable
fun BackfillScreen(onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val store = remember { koin.get<BackfillStore>() }
    val executor = remember { koin.get<BackfillExecutor>() }
    val runner = remember { koin.get<BackfillRunner>() }
    val gallery = remember { koin.get<GalleryProvider>() }
    val days = remember { koin.get<DayRepository>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }

    val batch by remember { store.batch }.collectAsState(null)
    val all by remember { days.observeAll() }.collectAsState(emptyList())

    var range by remember { mutableStateOf(BackfillRange.SINCE_START) }
    var usePhotos by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf<List<GalleryItem>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var off by remember { mutableStateOf(emptySet<YearMonth>()) }

    val candidates = remember(scanned, all, range, usePhotos) {
        val items = scanned ?: return@remember null
        val first = all.minOfOrNull { it.date } ?: today
        findCandidates(items, all.map { it.date }.toSet(), today, rangeStart(range, first, today), usePhotos)
    }
    val picked = remember(candidates, off) { candidates?.let { selectedCandidates(it, off) }.orEmpty() }

    val pick = rememberMultiMediaPicker { uris ->
        scanning = true
        scope.launch {
            val described = runCatching { gallery.describe(uris) }
            scanned = described.getOrDefault(emptyList())
            scanning = false
            val unreadable = uris.size - (scanned?.size ?: 0)
            if (described.isFailure) Notices.show("Couldn't read the selected items.")
            else if (unreadable > 0) Notices.show("Could not read $unreadable ${if (unreadable == 1) "item" else "items"}.")
        }
    }

    LaunchedEffect(batch?.finished) {
        if (batch?.finished == false) runner.start()
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Fill empty days", SliverType.DisplayM, Modifier.padding(start = SliverSpace.S2))
        }

        val running = batch
        if (running != null) {
            val filled = all.count { d -> d.source == DaySource.BACKFILL && running.items.any { it.date == d.date.toString() } }
            BatchProgress(running, filled, onUndo = {
                runner.cancel()
                scope.launch { executor.undoAll() }
            }, onDone = {
                scope.launch {
                    store.clear()
                    onBack()
                }
            })
            return@Column
        }

        SliverText(
            "Add days you never captured, using videos and photos you pick, by the date they were taken. " +
                "Everything stays on your phone, and days that already have a clip are never touched.",
            SliverType.Body, color = SliverColors.TextSecondary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            BackfillRange.entries.forEach { r -> Chip(r.label, range == r, { range = r }) }
        }
        SwitchRow("Use photos too", usePhotos, { usePhotos = it }, caption = "Days with only photos get a 1 s clip.")

        if (candidates == null) {
            PrimaryButton(if (scanning) "Reading dates..." else "Select photos and videos", {
                if (!scanning) pick()
            }, Modifier.fillMaxWidth())
            SliverText(
                "Select photos and videos from the days you missed.",
                SliverType.BodySmall, color = SliverColors.TextSecondary,
            )
        } else if (candidates.isEmpty()) {
            SliverText("Nothing to fill in this range.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            GhostButton("Select other items", { scanned = null }, Modifier.fillMaxWidth())
        } else {
            Mosaic(yearRows(candidates), off) { m -> off = if (m in off) off - m else off + m }
            PrimaryButton(fillLabel(picked.size), {
                if (picked.isEmpty()) return@PrimaryButton
                scope.launch {
                    val kept = picked.mapNotNull { item ->
                        runCatching { gallery.keep(item.uri) }.getOrNull()?.let { item.copy(uri = it) }
                    }
                    val failed = picked.size - kept.size
                    if (failed > 0) Notices.show("Could not read $failed ${if (failed == 1) "item" else "items"}.")
                    if (kept.isEmpty()) return@launch
                    store.start(kept)
                    runner.start()
                }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Mosaic(rows: List<YearRow>, off: Set<YearMonth>, onToggle: (YearMonth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        rows.forEach { row ->
            SliverText(row.year.toString(), SliverType.MonoS, color = SliverColors.TextTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
                row.counts.forEachIndexed { i, count ->
                    val month = YearMonth(row.year, i + 1)
                    val on = count > 0 && month !in off
                    Column(
                        Modifier.weight(1f).height(52.dp).clip(SliverShapes.S)
                            .background(if (on) SliverColors.AccentTint else SliverColors.Surface1)
                            .border(1.dp, if (on) SliverColors.AccentLine else SliverColors.LineHairline, SliverShapes.S)
                            .clickable(enabled = count > 0) { onToggle(month) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SliverText(MonthNames[i].take(1), SliverType.MonoS, color = SliverColors.TextTertiary)
                        SliverText(
                            if (count > 0) count.toString() else "-", SliverType.MonoS,
                            color = if (on) SliverColors.Accent else SliverColors.TextDisabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchProgress(batch: BackfillBatch, filled: Int, onUndo: () -> Unit, onDone: () -> Unit) {
    SliverText(
        if (batch.finished) "Filled $filled of ${batch.total} days" else "Filling ${batch.done} / ${batch.total}",
        SliverType.Title,
    )
    Box(Modifier.fillMaxWidth().height(2.dp).background(SliverColors.LineStrong)) {
        Box(Modifier.fillMaxWidth(batch.progress).height(2.dp).background(SliverColors.Accent))
    }
    if (!batch.finished) {
        SliverText(
            "This keeps going in the background, even if you close the app.",
            SliverType.BodySmall, color = SliverColors.TextSecondary,
        )
    }
    if (batch.finished) PrimaryButton("Done", onDone, Modifier.fillMaxWidth())
    GhostButton("Undo all", onUndo, Modifier.fillMaxWidth(), destructive = true)
}
