package app.sliver.feature.autoimport

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

    val empty = remember(from, rows, yesterday) { gapDays(from, yesterday) { it in rows }.size }
    val status = controller.status(PermissionKind.MediaLibrary)
    val usable = status == PermissionStatus.Granted || status == PermissionStatus.Partial

    fun begin() {
        if (starting) return
        starting = true
        scope.launch {
            val id = Uuid.random().toString()
            store.start(ImportRun(id = id, projectId = projects.activeId(), from = from.toString(), keep = keepNow, cameraOnly = !includeOther))
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

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Fill the calendar", SliverType.DisplayM, Modifier.padding(start = SliverSpace.S2))
        }

        val current = run
        if (current != null) {
            RunProgress(
                current, waiting.size, autoDays(waiting).size,
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

        SliverText(
            if (keepNow) "Fill empty days from your gallery: the best video of each day, or a photo if there is none."
            else "Fill empty days with a draft from your gallery. Drafts count only after you review and keep them.",
            SliverType.Body, color = SliverColors.TextSecondary,
        )

        SliverText("From", SliverType.Caption, color = SliverColors.TextTertiary)
        val presets = remember(start, today) { fromPresets(start, today) }
        val custom = presets.none { it.second == from }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            presets.forEach { (preset, date) ->
                Chip(presetLabel(preset), from == date, { fromRaw = date.toString() })
            }
            // DECISION (owner: "custom date is missing, simplify"): quick presets + one chip that opens the calendar and shows the picked date.
            Chip(if (custom) formatDay(from) else presetLabel(FromPreset.CUSTOM), custom, { pickerOpen = true })
        }
        SliverText(fillLabel(empty), SliverType.Headline, color = SliverColors.TextPrimary)
        SliverText("Since ${formatDay(from)} · filled days are never touched", SliverType.Meta, color = SliverColors.TextSecondary)

        if (status == PermissionStatus.Partial) {
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
        if (status == PermissionStatus.Denied) {
            SliverText(
                "Photo access is off. Turn it on in Settings so $APP_NAME can find a photo or video for each empty day.",
                SliverType.BodySmall, color = SliverColors.TextSecondary,
            )
            SecondaryButton("Open Settings", controller::openSettings)
        }

        SwitchRow(
            "Keep without review", keepNow, { keepNow = it },
            caption = "Each day gets its best video or photo right away. You can still swap any day later.",
        )
        SwitchRow(
            "Include saved and received media", includeOther, { includeOther = it },
            caption = "Off: only photos and videos from your camera. Screenshots are always skipped.",
        )
        PrimaryButton(
            if (starting) "Starting..." else if (keepNow) "Fill $empty ${if (empty == 1) "day" else "days"}" else "Import drafts",
            {
                if (usable) begin() else {
                    wantStart = true
                    controller.request(PermissionKind.MediaLibrary)
                }
            },
            Modifier.fillMaxWidth(), enabled = empty > 0 && !starting,
        )
    }

    DatePickerSheet(pickerOpen, from, LocalDate(today.year - 10, 1, 1), today, { fromRaw = it.toString(); pickerOpen = false }, { pickerOpen = false })
}

@Composable
private fun RunProgress(run: ImportRun, draftsLeft: Int, autoLeft: Int, onCancel: () -> Unit, onReview: () -> Unit, onKeepAll: () -> Unit, onRemoveUnchecked: () -> Unit, onDone: () -> Unit, onUndo: () -> Unit) {
    SliverText(
        when {
            run.finished -> importSummary(run.done, (run.total - run.done).coerceAtLeast(0), run.keep)
            run.keep -> "Filling days"
            else -> "Importing drafts"
        },
        SliverType.Title,
    )
    if (!run.finished) {
        SliverText(
            when {
                run.scanning -> "Looking through your gallery"
                run.total == 0 -> "Nothing to fill"
                else -> "${run.done} of ${run.total} days"
            },
            SliverType.Headline, color = SliverColors.TextPrimary,
        )
    }
    Box(Modifier.fillMaxWidth().height(4.dp).clip(SliverShapes.Pill).background(SliverColors.LineStrong)) {
        Box(Modifier.fillMaxWidth(if (run.finished) 1f else run.progress).height(4.dp).clip(SliverShapes.Pill).background(SliverColors.Accent))
    }
    // Owner: "needs some progress stats here".
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        if (run.gapDays > 0) StatRow("Empty days since ${formatDay(LocalDate.parse(run.from))}", groupThousands(run.gapDays))
        StatRow("Found in your gallery", if (run.scanning) "Searching…" else "${groupThousands(run.scanned)} items")
        if (!run.scanning) StatRow("Days with something to use", groupThousands(run.total))
        if (run.videos + run.photos > 0) StatRow("Filled with", "${run.videos} ${if (run.videos == 1) "video" else "videos"} · ${run.photos} ${if (run.photos == 1) "photo" else "photos"}")
        val first = run.firstFilled?.let(LocalDate::parse)
        val last = run.lastFilled?.let(LocalDate::parse)
        if (first != null && last != null) StatRow("Filled days", "${formatDay(first)} – ${formatDay(last)}")
        if (run.finished && first != null && first > LocalDate.parse(run.from)) {
            SliverText(
                "Your gallery has no photos or videos with a date before ${formatDay(first)}, so earlier days stay empty.",
                SliverType.BodySmall, color = SliverColors.TextSecondary,
            )
        }
        if (run.skipped > 0) StatRow("Couldn't read", "${run.skipped} ${if (run.skipped == 1) "day" else "days"}")
        if (!run.finished) run.current?.let { StatRow("Now", formatDay(LocalDate.parse(it))) }
        if (!run.finished) etaLabel(run.done, run.total, run.startedAt, now)?.let { StatRow("Time", it) }
    }
    if (!run.finished) {
        SliverText("Keeps going in the background.", SliverType.BodySmall, color = SliverColors.TextSecondary)
        GhostButton("Cancel", onCancel, Modifier.fillMaxWidth())
    } else {
        if (draftsLeft > 0) {
            PrimaryButton("Keep all $draftsLeft", onKeepAll, Modifier.fillMaxWidth())
            SecondaryButton("Review one by one", onReview, Modifier.fillMaxWidth())
            if (autoLeft > 0) GhostButton("Remove unchecked", onRemoveUnchecked, Modifier.fillMaxWidth(), destructive = true)
        }
        SecondaryButton("Done", onDone)
        GhostButton("Undo all", onUndo, Modifier.fillMaxWidth(), destructive = true)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SliverText(label, SliverType.BodySmall, Modifier.weight(1f), color = SliverColors.TextSecondary)
        SliverText(value, SliverType.Num, color = SliverColors.TextPrimary)
    }
}
