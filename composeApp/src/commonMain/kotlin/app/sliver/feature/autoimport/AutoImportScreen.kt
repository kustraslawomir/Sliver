package app.sliver.feature.autoimport

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import app.sliver.data.db.Day
import app.sliver.feature.archive.MonthLong
import app.sliver.feature.archive.weekdayName
import app.sliver.feature.trim.rememberPhoto
import app.sliver.core.APP_NAME

import app.sliver.core.ui.SwitchRow
import androidx.compose.foundation.layout.FlowRow
import app.sliver.core.ui.Chip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.feature.gallery.formatDay
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import app.sliver.core.navigation.LocalNavigator
import app.sliver.core.ui.Notices
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.archive.YearMonth
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AutoImportScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val store = remember { koin.get<AutoImportStore>() }
    val executor = remember { koin.get<AutoImportExecutor>() }
    val runner = remember { koin.get<AutoImportRunner>() }
    val days = remember { koin.get<DayRepository>() }
    val projects = remember { koin.get<ProjectRepository>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }
    val yesterday = remember { LocalDate.fromEpochDays(today.toEpochDays() - 1) }
    val controller = rememberPermissionController()
    val draftNav = rememberDraftNav()

    val run by remember { store.run }.collectAsState(null)
    val project by remember { projects.active }.collectAsState(null)
    val rows by remember { combine(days.observeAll(), days.observeDrafts()) { a, d -> (a + d).map { it.date }.toSet() } }.collectAsState(emptySet())
    val waiting by remember { combine(days.observeDrafts(), days.observeAll(), ::toCheck) }.collectAsState(emptyList())
    val navigator = LocalNavigator.current
    var sawRunning by rememberSaveable { mutableStateOf(false) }

    val start = project?.startDate ?: today
    var fromRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val from = fromRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start
    var pickerOpen by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var wantStart by rememberSaveable { mutableStateOf(false) }
    var keepNow by rememberSaveable { mutableStateOf(true) }
    var includeOther by rememberSaveable { mutableStateOf(false) }

    val folderChoice = rememberFolderChoice()
    val empty = remember(from, rows, yesterday) { gapDays(from, yesterday) { it in rows }.size }
    val status = controller.status(PermissionKind.MediaLibrary)
    val usable = status == PermissionStatus.Granted || status == PermissionStatus.Partial

    fun begin() {
        if (starting) return
        starting = true
        scope.launch {
            val id = Uuid.random().toString()
            store.start(ImportRun(id = id, projectId = projects.activeId(), from = from.toString(), keep = keepNow, cameraOnly = !includeOther, folder = folderChoice.tree, folderName = folderChoice.name))
            runner.start()
            starting = false
        }
    }

    LaunchedEffect(usable, wantStart) {
        if (usable && wantStart) {
            wantStart = false
            begin()
        }
    }
    LaunchedEffect(run?.finished) {
        val r = run ?: return@LaunchedEffect
        if (!r.finished) {
            sawRunning = true
            runner.start()
        } else if (sawRunning) {
            sawRunning = false
            if (r.keep && r.done > 0) {
                days.draftsOfRun(r.projectId, r.id).minOfOrNull { it.date }?.let {
                    ArchiveSession.pendingMonth = YearMonth(it.year, it.month.ordinal + 1)
                }
                Notices.show(filledToast(r.done))
                navigator.goHome()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = SliverSpace.S4), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Fill the calendar", SliverType.DisplayM, Modifier.padding(start = SliverSpace.S2))
        }

        val current = run
        if (current != null) {
            RunProgress(
                current, waiting, autoDays(waiting).size,
                onCancel = {
                    runner.cancel()
                    scope.launch { store.update(current.id) { it.copy(finished = true) } }
                },
                onReview = { draftNav.openFirst(onNavigate) },
                onKeepAll = { draftNav.keepAll() },
                onRemoveUnchecked = { draftNav.removeUnchecked() },
                onDone = { scope.launch { store.clear() } },
                onUndo = {
                    runner.cancel()
                    scope.launch { executor.undoAll() }
                },
            )
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
        ) {
            SliverText(
                if (keepNow) "Picks one clip for every empty day from your camera roll \u2014 the longest video, else a photo. Nothing leaves your phone."
                else "Fill empty days with a draft from your gallery. Drafts count only after you review and keep them.",
                SliverType.Body, color = SliverColors.TextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                SliverText("From", SliverType.BodySmall, color = SliverColors.TextTertiary)
                val presets = remember(start, today) { fromPresets(start, today) }
                val custom = presets.none { it.second == from }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    presets.forEach { (preset, date) ->
                        Chip(presetLabel(preset), from == date, { fromRaw = date.toString() })
                    }
                    // DECISION (owner: "custom date is missing, simplify"): quick presets + one chip that opens the calendar and shows the picked date.
                    Chip(if (custom) formatDay(from) else presetLabel(FromPreset.CUSTOM), custom, { pickerOpen = true })
                }
            }
            FolderSourcePicker(folderChoice)
            BigCount(groupThousands(empty), null, "empty ${if (empty == 1) "day" else "days"} to fill \u00B7 since ${from.day} ${MonthLong[from.month.ordinal]}")

            if (!folderChoice.useFolder && status == PermissionStatus.Partial) {
                Column(
                    Modifier.fillMaxWidth().clip(SliverShapes.R12).background(SliverColors.Surface1)
                        .border(1.dp, SliverColors.LineStrong, SliverShapes.R12).padding(SliverSpace.S4),
                    verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
                ) {
                    SliverText("Limited access", SliverType.Headline, color = SliverColors.TextPrimary)
                    SliverText(
                        "$APP_NAME can only see the photos and videos you selected, so most days may stay empty.",
                        SliverType.BodySmall, color = SliverColors.TextSecondary,
                    )
                    SecondaryButton("Allow all", { controller.request(PermissionKind.MediaLibrary) })
                }
            }
            if (!folderChoice.useFolder && status == PermissionStatus.Denied) {
                SliverText(
                    "Photo access is off. Turn it on in Settings so $APP_NAME can find a photo or video for each empty day.",
                    SliverType.BodySmall, color = SliverColors.TextSecondary,
                )
                SecondaryButton("Open Settings", controller::openSettings)
            }

            Column {
                Hairline()
                SwitchRow(
                    "Keep without review", keepNow, { keepNow = it },
                    caption = "Filled days count right away. You can check them later.",
                )
                Hairline()
                SwitchRow(
                    "Include saved and received media", includeOther, { includeOther = it },
                    caption = "Off: only photos and videos taken with your camera.",
                )
            }
        }
        Column(
            Modifier.padding(horizontal = SliverSpace.Gutter).padding(bottom = SliverSpace.S4),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SliverText(
                "Screenshots and screen recordings are always skipped. Asks for gallery access once.",
                SliverType.Caption.copy(textAlign = TextAlign.Center), color = SliverColors.TextTertiary,
            )
            PrimaryButton(
                if (starting) "Starting..." else if (keepNow) "Fill $empty ${if (empty == 1) "day" else "days"}" else "Import drafts",
                {
                    if (folderChoice.useFolder || usable) begin() else {
                        wantStart = true
                        controller.request(PermissionKind.MediaLibrary)
                    }
                },
                Modifier.fillMaxWidth(), enabled = empty > 0 && !starting && (!folderChoice.useFolder || folderChoice.ready),
            )
        }
    }

    DatePickerSheet(pickerOpen, from, LocalDate(today.year - 10, 1, 1), today, { fromRaw = it.toString(); pickerOpen = false }, { pickerOpen = false })
}

/** Display-size number with an optional dimmer tail ("64 of 143") and a caption under it. */
@Composable
private fun BigCount(number: String, tail: String?, caption: String) {
    Column {
        SliverText(
            buildAnnotatedString {
                append(number)
                if (tail != null) withStyle(SpanStyle(color = SliverColors.TextDisabled)) { append(" $tail") }
            },
            SliverType.DisplayXL, color = SliverColors.TextPrimary, maxLines = 1,
        )
        SliverText(caption, SliverType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp), color = SliverColors.TextTertiary)
    }
}

@Composable
private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))

private fun shortDay(d: LocalDate) = "${d.day} ${MonthLong[d.month.ordinal].take(3)}"

@Composable
private fun ColumnScope.RunProgress(
    run: ImportRun, waiting: List<Day>, autoLeft: Int,
    onCancel: () -> Unit, onReview: () -> Unit, onKeepAll: () -> Unit, onRemoveUnchecked: () -> Unit, onDone: () -> Unit, onUndo: () -> Unit,
) {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val first = run.firstFilled?.let(LocalDate::parse)
    val last = run.lastFilled?.let(LocalDate::parse)
    val from = LocalDate.parse(run.from)
    val nothingBefore = first != null && first > from
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        if (!run.finished) {
            when {
                run.scanning -> BigCount("\u2026", null, if (run.folder != null) "Looking through your folder" else "Looking through your gallery")
                else -> BigCount(
                    groupThousands(run.done), "of ${groupThousands(run.total)}",
                    "days filled" + (etaLabel(run.done, run.total, run.startedAt, now)?.let { " \u00B7 ${it.replaceFirstChar(Char::lowercase)}" } ?: ""),
                )
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(SliverShapes.Pill).background(SliverColors.LineStrong)) {
                Box(Modifier.fillMaxWidth(run.progress).height(4.dp).clip(SliverShapes.Pill).background(SliverColors.TextPrimary))
            }
            run.current?.let {
                val d = LocalDate.parse(it)
                SliverText("Now: ${weekdayName(d)} ${d.day} ${MonthLong[d.month.ordinal]}", SliverType.BodySmall, color = SliverColors.TextSecondary)
            }
            // Owner: "needs some progress stats here".
            Column {
                if (!run.scanning) StatRow("Days with media", groupThousands(run.total))
                StatRow("Items found", if (run.scanning) (if (run.found > 0) "${groupThousands(run.found)} files" else "Searching\u2026") else groupThousands(run.scanned))
                if (run.videos + run.photos > 0) StatRow("Videos \u00B7 photos", "${run.videos} \u00B7 ${run.photos}")
                if (!run.scanning && run.gapDays > 0) StatRow("Still empty", groupThousands((run.gapDays - run.total).coerceAtLeast(0)))
                if (run.skipped > 0) StatRow("Couldn't read", groupThousands(run.skipped))
                if (first != null && last != null) StatRow("Filled so far", "${shortDay(first)} \u2013 ${shortDay(last)}")
                Hairline()
            }
            SliverText(
                "You can leave this screen." + if (nothingBefore) " Your ${if (run.folder != null) "folder" else "gallery"} has nothing before ${formatDay(first!!)}." else "",
                SliverType.BodySmall, color = SliverColors.TextTertiary,
            )
            FolderRunNotes(run)
        } else {
            BigCount(
                groupThousands(run.done), null,
                (if (run.keep) "days filled" else "drafts added") + if (first != null && last != null) " \u00B7 ${shortDay(first)} \u2013 ${shortDay(last)}" else "",
            )
            val mine = waiting.filter { it.importRunId == run.id }.take(40)
            if (mine.isNotEmpty()) Mosaic(mine)
            val stayEmpty = (run.gapDays - run.done).coerceAtLeast(0)
            SliverText(
                buildString {
                    if (stayEmpty > 0) append("$stayEmpty ${if (stayEmpty == 1) "day" else "days"} had nothing in your ${if (run.folder != null) "folder" else "gallery"} and stay empty. ")
                    if (run.skipped > 0) append("${run.skipped} couldn't be read. ")
                    append(if (run.keep) "Filled days are marked until you check them." else "Drafts count only after you keep them.")
                },
                SliverType.BodySmall, color = SliverColors.TextTertiary,
            )
            FolderRunNotes(run)
        }
    }
    Column(
        Modifier.padding(horizontal = SliverSpace.Gutter).padding(bottom = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        if (!run.finished) {
            SecondaryButton("Stop", onCancel, Modifier.fillMaxWidth())
        } else {
            if (waiting.isNotEmpty()) {
                PrimaryButton("Keep all", onKeepAll, Modifier.fillMaxWidth())
                SecondaryButton("Review one by one", onReview, Modifier.fillMaxWidth())
            } else {
                PrimaryButton("Done", onDone, Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (autoLeft > 0) GhostButton("Remove unchecked", onRemoveUnchecked, destructive = true) else Spacer(Modifier.size(1.dp))
                GhostButton("Undo all", onUndo)
            }
        }
    }
}

/** 8-column grid of the run's filled days (board FillCalendarDone). */
@Composable
private fun Mosaic(days: List<Day>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(8).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { d ->
                    val thumb = rememberPhoto(d.thumb)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                            .background(d.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3),
                    ) { if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Hairline()
    Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
        SliverText(label, SliverType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp), Modifier.weight(1f), color = SliverColors.TextSecondary)
        SliverText(value, SliverType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum"), color = SliverColors.TextPrimary)
    }
}
