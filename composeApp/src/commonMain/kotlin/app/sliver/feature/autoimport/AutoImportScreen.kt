package app.sliver.feature.autoimport

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
    val drafts by remember { days.observeDrafts() }.collectAsState(emptyList())

    val start = project?.startDate ?: today
    var fromRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val from = fromRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start
    var pickerOpen by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var wantStart by rememberSaveable { mutableStateOf(false) }

    val empty = remember(from, rows, yesterday) { gapDays(from, yesterday) { it in rows }.size }
    val status = controller.status(PermissionKind.MediaLibrary)
    val usable = status == PermissionStatus.Granted || status == PermissionStatus.Partial

    fun begin() {
        if (starting) return
        starting = true
        scope.launch {
            val id = Uuid.random().toString()
            store.start(ImportRun(id = id, projectId = projects.activeId(), from = from.toString()))
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
        if (run?.finished == false) runner.start()
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Auto-import", SliverType.DisplayM, Modifier.padding(start = SliverSpace.S2))
        }

        val current = run
        if (current != null) {
            RunProgress(
                current, drafts.size,
                onCancel = {
                    runner.cancel()
                    scope.launch { store.update(current.id) { it.copy(finished = true) } }
                },
                onReview = { draftNav.openFirst(onNavigate) },
                onDone = { scope.launch { store.clear() } },
                onUndo = {
                    runner.cancel()
                    scope.launch { executor.undoAll() }
                },
            )
            return@Column
        }

        SliverText(
            "Fill empty days with a draft from your gallery. Drafts count only after you review and keep them.",
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
                    "Fragments can only see the photos and videos you selected, so most days may stay empty.",
                    SliverType.BodySmall, color = SliverColors.TextSecondary,
                )
                SecondaryButton("Allow all", { controller.request(PermissionKind.MediaLibrary) })
            }
        }
        if (status == PermissionStatus.Denied) {
            SliverText(
                "Photo access is off. Turn it on in Settings so Fragments can find a photo or video for each empty day.",
                SliverType.BodySmall, color = SliverColors.TextSecondary,
            )
            SecondaryButton("Open Settings", controller::openSettings)
        }

        PrimaryButton(
            if (starting) "Starting..." else "Import drafts",
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
private fun RunProgress(run: ImportRun, draftsLeft: Int, onCancel: () -> Unit, onReview: () -> Unit, onDone: () -> Unit, onUndo: () -> Unit) {
    SliverText(
        if (run.finished) importSummary(run.done, (run.total - run.done).coerceAtLeast(0)) else "Importing drafts",
        SliverType.Title,
    )
    if (!run.finished) {
        SliverText(
            if (run.total == 0) "Looking through your gallery" else "${run.done} of ${run.total} days",
            SliverType.Meta, color = SliverColors.TextSecondary,
        )
    }
    Box(Modifier.fillMaxWidth().height(2.dp).background(SliverColors.LineStrong)) {
        Box(Modifier.fillMaxWidth(if (run.finished) 1f else run.progress).height(2.dp).background(SliverColors.Accent))
    }
    if (!run.finished) {
        SliverText("This keeps going in the background, even if you close the app.", SliverType.BodySmall, color = SliverColors.TextSecondary)
        GhostButton("Cancel", onCancel, Modifier.fillMaxWidth())
    } else {
        if (draftsLeft > 0) PrimaryButton("Review drafts", onReview, Modifier.fillMaxWidth())
        SecondaryButton("Done", onDone)
        GhostButton("Undo all", onUndo, Modifier.fillMaxWidth(), destructive = true)
    }
}
