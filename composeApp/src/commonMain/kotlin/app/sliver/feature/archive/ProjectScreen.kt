package app.sliver.feature.archive
import app.sliver.feature.camera.label
import app.sliver.core.ui.SheetsOpen
import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

import app.sliver.core.kit.GlassStyle
import app.sliver.core.kit.glass
import dev.chrisbanes.haze.hazeSource

import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton

import app.sliver.core.theme.ReelogType

import app.sliver.feature.camera.liquidGlow
import androidx.compose.animation.core.animateFloat
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.PrimaryButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.changedOnAnotherPhone
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import app.sliver.feature.camera.overMedia
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.HubIcons
import app.sliver.core.icons.SliverIcons
import app.sliver.core.icons.SliverIcon
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.monthGrid
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.EdgeState
import app.sliver.core.ui.LocalWindowSize
import app.sliver.core.ui.ClipDrop
import app.sliver.core.ui.MaxContentWidth
import app.sliver.core.ui.NoticeSheet
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.rememberTapGate
import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.autoimport.ReviewSheetBody
import app.sliver.feature.autoimport.dashedOutline
import app.sliver.feature.autoimport.autoDays
import app.sliver.feature.autoimport.decodeCandidates
import app.sliver.feature.autoimport.nextDraft
import app.sliver.feature.autoimport.reviewChipLabel
import app.sliver.feature.autoimport.toCheck
import app.sliver.feature.autoimport.rememberDraftNav
import app.sliver.feature.camera.CaptureSession
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import app.sliver.feature.films.FilmActionsSheet
import app.sliver.feature.films.FilmsSection
import app.sliver.feature.gallery.rememberAddMedia
import app.sliver.feature.projects.ProjectAction
import app.sliver.feature.projects.ProjectsSheet
import app.sliver.feature.projects.canLeaveActive
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.rememberPhoto
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.koin.mp.KoinPlatform

/** Date handed to the Day detail screen until it takes a route argument. */
object ArchiveSession {
    var selected: LocalDate? = null

    /** Month the calendar should show next time it opens (set after an auto-fill run). */
    var pendingMonth: YearMonth? = null
}

private val WeekDays = listOf("M", "T", "W", "T", "F", "S", "S")
private val Weekdays3 = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val CellShape get() = SliverShapes.S

@Composable
fun ProjectScreen(onNavigate: (Route) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val today = remember { todayLocal() }
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)
    val byDate = remember(all) { all.associateBy { it.date } }
    val syncing by remember { koin.get<SyncEngine>().progress }.collectAsState()
    val drafts by remember { days.observeDrafts() }.collectAsState(emptyList())
    val calendar = remember(byDate, drafts) { drafts.associateBy { it.date } + byDate }
    val waiting = remember(drafts, all) { toCheck(drafts, all) }
    val autoCount = remember(all) { autoDays(all).size }
    val draftNav = rememberDraftNav()
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    val altMap by remember { koin.get<app.sliver.feature.autoimport.AutoImportStore>().alternatives }.collectAsState(emptyMap())
    val alts = remember(altMap, project) {
        val prefix = "${project?.id}|"
        altMap.filter { it.key.startsWith(prefix) && it.value.isNotEmpty() }.mapNotNull { runCatching { LocalDate.parse(it.key.removePrefix(prefix)) }.getOrNull() to it.value.size }
            .mapNotNull { (d, n) -> d?.let { it to n } }.toMap()
    }
    val projects by remember { koin.get<ProjectRepository>().projects }.collectAsState(emptyList())
    // Auto-import can fill days before the project was created: the calendar (and "since") must reach them.
    val first = listOfNotNull(project?.startDate ?: app?.firstDay, all.minOfOrNull { it.date }, drafts.minOfOrNull { it.date }).minOrNull() ?: today

    val controller = rememberPermissionController()
    val scope = rememberCoroutineScope()
    val undo = remember { koin.get<SaveUndo>() }
    var notice by remember { mutableStateOf<String?>(null) }
    var filmMenu by remember { mutableStateOf<app.sliver.data.db.Film?>(null) }
    var shownIndex by rememberSaveable { mutableStateOf(today.toYearMonth().let { it.year * 12 + it.month - 1 }) }
    val shown = YearMonth(shownIndex / 12, shownIndex % 12 + 1)
    fun show(m: YearMonth) { shownIndex = m.year * 12 + m.month - 1 }
    var addFor by remember { mutableStateOf<LocalDate?>(null) }
    var draftsSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ArchiveSession.pendingMonth?.let { show(it); ArchiveSession.pendingMonth = null } }
    var projectsOpen by remember { mutableStateOf(false) }
    var projectAction by remember { mutableStateOf<ProjectAction?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var manageOpen by remember { mutableStateOf(false) }

    val wide = !LocalWindowSize.current.isCompact
    var paneDay by rememberSaveable { mutableStateOf<String?>(null) }
    val pane = paneDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun openDay(date: LocalDate) {
        ArchiveSession.selected = date
        if (wide) paneDay = date.toString() else onNavigate(Route.DayDetail)
    }

    fun onCell(date: LocalDate) {
        when (cellState(date, today, calendar[date], first)) {
            CellState.FUTURE -> Unit
            CellState.TODAY -> {
                CaptureSession.forDate = null
                onNavigate(Route.Camera)
            }
            CellState.EMPTY -> addFor = date
            CellState.FILLED, CellState.BACKFILLED -> openDay(date)
            // Owner: no in-between sheet — the editor already lists the day's other items, Keep and Remove.
            CellState.DRAFT, CellState.AUTO -> calendar[date]?.let { draftNav.openFirst(onNavigate, it, chain = false) }
        }
    }

    val addMedia = rememberAddMedia(onNavigate)

    fun replaceDay(date: LocalDate) {
        CaptureSession.forDate = date
        if (date == today) onNavigate(Route.Camera) else addMedia(date)
    }

    fun removeDay(date: LocalDate) {
        val d = byDate[date] ?: return
        scope.launch { if (runCatching { undo.remove(d) }.isFailure) notice = "Couldn't remove this clip. Try again." }
    }

    val changed = app?.account?.changedNotices.orEmpty()
    LaunchedEffect(changed, project?.id) {
        val id = project?.id ?: return@LaunchedEffect
        val mine = changed.filter { it.substringBefore('|') == id }
        val text = changedOnAnotherPhone(mine.mapNotNull { runCatching { LocalDate.parse(it.substringAfter('|')) }.getOrNull() })
        if (text != null && notice == null) {
            notice = text
            settings.update { s -> s.copy(account = s.account.copy(changedNotices = s.account.changedNotices - mine.toSet())) }
        }
    }

    val reminderOff = app?.reminder?.enabled == false ||
        controller.status(PermissionKind.Notifications) != PermissionStatus.Granted
    val showNudge = app != null && !app!!.nudgeDismissed && reminderOff

    val dockBackdrop = dev.chrisbanes.haze.rememberHazeState()
    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Row(Modifier.fillMaxSize().hazeSource(dockBackdrop), horizontalArrangement = Arrangement.Center) {
            Column(
                (if (wide) Modifier.weight(1f) else Modifier.widthIn(max = MaxContentWidth).fillMaxWidth())
                    .fillMaxHeight().navigationBarsPadding(),
            ) {
                Header(
                    project?.name.orEmpty(), clipsHeader(all.size, first) + (project?.let { " · " + it.format.label() } ?: ""), menuOpen,
                    { projectsOpen = true }, { onNavigate(Route.Settings) }, { menuOpen = true },
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = RecordDockHeight),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SyncStrip(syncing != null)
                    Column(
                        Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (waiting.isNotEmpty()) ReviewRow(reviewChipLabel(drafts.size, autoCount)) { draftsSheet = true }

                        if (all.isEmpty() && app != null) {
                            EdgeState(
                                "Your year starts today", "Record one clip a day. Your archive fills in here.",
                                Modifier.fillMaxWidth(), "Record today's clip",
                            ) { CaptureSession.forDate = null; onNavigate(Route.Camera) }
                        }

                        if (showNudge) {
                            NudgeCard(
                                onSet = {
                                    scope.launch { settings.update { it.copy(reminder = it.reminder.copy(enabled = true)) } }
                                    if (controller.status(PermissionKind.Notifications) == PermissionStatus.NotAsked) controller.request(PermissionKind.Notifications)
                                    onNavigate(Route.Reminders)
                                },
                                onDismiss = { scope.launch { settings.update { it.copy(nudgeDismissed = true) } } },
                            )
                        }

                        val count = remember(all, shown) { all.count { it.date.year == shown.year && it.date.month.ordinal + 1 == shown.month } }
                        MonthHeading(
                            MonthLong[shown.month - 1], "$count ${if (count == 1) "clip" else "clips"}",
                            canGoPrev(shown, first), canGoNext(shown, today),
                            { show(stepMonth(shown, -1, first, today)) }, { show(stepMonth(shown, 1, first, today)) },
                        )
                        Box(
                            Modifier.pointerInput(shown, first, today) {
                                var dx = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { dx = 0f },
                                    onDragEnd = {
                                        if (dx > SwipePx) show(stepMonth(shown, -1, first, today))
                                        else if (dx < -SwipePx) show(stepMonth(shown, 1, first, today))
                                    },
                                    onDragCancel = { dx = 0f },
                                ) { _, delta -> dx += delta }
                            },
                        ) { MonthGrid(shown, calendar, today, first, ::onCell, ::replaceDay, ::removeDay, alts) }
                    }
                    if (!wide) FilmsSection(onNavigate, { filmMenu = it })
                }
            }
            if (wide) {
                Box(Modifier.weight(1.3f).fillMaxHeight()) {
                    if (pane != null) {
                        key(pane) { DayDetailScreen(onNavigate, onBack = { paneDay = null }) }
                    } else {
                        Column(
                            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                                .padding(vertical = SliverSpace.S4),
                        ) { FilmsSection(onNavigate, { filmMenu = it }) }
                    }
                }
            }
        }

        val minute by produceState(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time) {
            // Caption counts hours left; refresh once a minute (no animation, ~0 CPU).
            while (true) { delay(60_000); value = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
        }
        if (SheetsOpen.count == 0 && !menuOpen) RecordDock(
            today = byDate[today],
            caption = dockCaption(today, minute.hour, minute.minute),
            backdrop = dockBackdrop,
            onRecord = { CaptureSession.forDate = null; onNavigate(Route.Camera) },
            onOpenToday = { openDay(today) },
            modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = MaxContentWidth),
        )
        ProjectsSheet(projectsOpen, { projectsOpen = false }, backdrop = dockBackdrop)
        ProjectsSheet(projectAction != null, { projectAction = null }, action = projectAction)
        NoticeSheet(notice) { notice = null }
        FilmActionsSheet(filmMenu) { filmMenu = null }
        if (menuOpen) {
            val canLeave = project?.let { canLeaveActive(it, projects) } ?: false
            ProjectMenu(
                canLeave,
                onDismiss = { menuOpen = false },
                onRename = { menuOpen = false; projectAction = ProjectAction.Rename },
                onShape = { menuOpen = false; projectAction = ProjectAction.Format },
                onFill = { menuOpen = false; onNavigate(Route.FillThePast) },
                onManage = { menuOpen = false; manageOpen = true },
            )
        }
        SliverBottomSheet(manageOpen, onDismiss = { manageOpen = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText("Archive or delete", SliverType.Title)
                SliverText(
                    "Archive hides the project from your list and keeps everything. You can bring it back from Projects.",
                    SliverType.BodySmall, color = SliverColors.TextSecondary,
                )
                SecondaryButton("Archive project", {
                    manageOpen = false
                    project?.let { p -> scope.launch { koin.get<ProjectRepository>().setArchived(p.id, true) } }
                }, Modifier.fillMaxWidth())
                GhostButton(
                    "Delete project\u2026", { manageOpen = false; projectAction = ProjectAction.Delete }, Modifier.fillMaxWidth(), destructive = true,
                )
            }
        }
        SliverBottomSheet(draftsSheet && waiting.isNotEmpty(), onDismiss = { draftsSheet = false }) {
            ReviewSheetBody(
                drafts.size, autoCount, draftNav.busy,
                onKeepAll = { draftsSheet = false; draftNav.keepAll() },
                onRemoveUnchecked = { draftsSheet = false; draftNav.removeUnchecked() },
                // Owner: no day sheet; the editor walks the days itself (previous / next).
                onWalk = { draftsSheet = false; waiting.firstOrNull()?.let { draftNav.openFirst(onNavigate, it, chain = true) } },
                days = waiting,
            )
        }
        SliverBottomSheet(addFor != null, onDismiss = { addFor = null }) {
            val date = addFor ?: return@SliverBottomSheet
            AddSheet(date, { addFor = null; addMedia(date) }, { addFor = null })
        }
    }
}

private val hubTitle get() = SliverType.DisplayM

@Composable
private fun Header(
    name: String, line: String, menuOpen: Boolean,
    onSwitch: () -> Unit, onSettings: () -> Unit, onMore: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(SliverColors.BgBase).statusBarsPadding().padding(top = 11.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier.weight(1f).height(44.dp).padding(start = 12.dp).clickable(role = Role.Button, onClick = rememberTapGate(onSwitch))
                    .semantics { contentDescription = "Switch project" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SliverText(name, hubTitle, Modifier.weight(1f, fill = false), maxLines = 1)
                SliverIcon(HubIcons.Switcher, SliverColors.TextTertiary, size = 16.dp)
            }
            // HubV17: settings gear, then "…" for the project menu; 24 dp Lucide, no backgrounds.
            HubButton(SliverIcons.Settings, "Settings", onSettings, SliverColors.TextPrimary, 24.dp)
            HubButton(SliverIcons.MoreHorizontal, "Project menu", onMore, SliverColors.TextPrimary, 24.dp, if (menuOpen) SliverColors.Surface3 else Color.Transparent)
        }
        SliverText(line, SliverType.Body.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label), color = SliverColors.TextTertiary), Modifier.padding(start = 20.dp, top = 4.dp), maxLines = 1)
        Box(Modifier.height(20.dp))
    }
}

@Composable
private fun HubButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color, size: Dp, bg: Color = Color.Transparent) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = rememberTapGate(onClick)),
        Alignment.Center,
    ) { SliverIcon(icon, tint, size = size) }
}

@Composable
private fun NavArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        Alignment.Center,
    ) { SliverIcon(icon, if (enabled) SliverColors.TextPrimary else SliverColors.LineStrong, size = 18.dp) }
}

@Composable
private fun NudgeCard(onSet: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(SliverShapes.R20).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.R20).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SliverIcon(HubIcons.Bell, SliverColors.AccentText)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SliverText("Want a daily nudge?", SliverType.Headline.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)))
                SliverText("One quiet reminder at a time you choose.", SliverType.Caption, color = SliverColors.TextTertiary)
            }
        }
        Row(Modifier.padding(start = 34.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReelogButton("Set reminder", onSet, small = true, fullWidth = false)
            ReelogButton("No thanks", onDismiss, kind = ButtonKind.Secondary, small = true, fullWidth = false)
        }
    }
}

private const val SwipePx = 80f

@Composable
private fun MonthHeading(left: String, right: String, canPrev: Boolean, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SliverText(left, SliverType.DisplayM.copy(fontSize = ReelogType.Size.title, lineHeight = ReelogType.lineFor(ReelogType.Size.title)), maxLines = 1, softWrap = false)
        SliverText(right, SliverType.tnum(0.06.em, SliverColors.TextTertiary), Modifier.weight(1f), maxLines = 1, softWrap = false)
        NavArrow(HubIcons.Prev, "Previous month", canPrev, onPrev)
        NavArrow(HubIcons.Next, "Next month", canNext, onNext)
    }
}

@Composable
private fun MonthGrid(
    shown: YearMonth, byDate: Map<LocalDate, Day>, today: LocalDate, start: LocalDate,
    onCell: (LocalDate) -> Unit, onReplace: (LocalDate) -> Unit, onRemove: (LocalDate) -> Unit,
    alts: Map<LocalDate, Int> = emptyMap(),
) {
    val weeks = remember(shown) { monthGrid(shown.year, shown.month).filter { week -> week.any { it != null } } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WeekDays.forEach {
                SliverText(it, SliverType.tnum(color = SliverColors.TextTertiary).copy(textAlign = TextAlign.Center), Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(58.dp)) {
                            if (date != null) DayCell(date, today, start, byDate[date], { onReplace(date) }, { onRemove(date) }, alts[date] ?: 0) { onCell(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate, today: LocalDate, start: LocalDate, day: Day?, onReplace: () -> Unit, onRemove: () -> Unit, others: Int = 0,
    onClick: () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(day?.takenAt) {
        if (day != null && ClipDrop.pending == date) {
            ClipDrop.clear()
            if (!reduced) {
                scale.snapTo(0.6f)
                scale.animateTo(1f, tween(420, easing = SliverMotion.EaseEmphasized))
            }
        }
    }
    val state = cellState(date, today, day, start)
    val autoItems = if (state == CellState.AUTO) remember(day?.draftCandidates) { decodeCandidates(day?.draftCandidates).size.coerceAtLeast(1) } else null
    val base = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CellShape)
    val surface = when (state) {
        CellState.FUTURE -> base
        CellState.EMPTY -> base.background(SliverColors.Surface1).border(1.dp, SliverColors.CellEmptyLine, CellShape)
        CellState.TODAY -> base.border(2.dp, SliverColors.AccentMark, CellShape)
        // Owner: a picked day has no border at all on return; only the pop-in shows where it landed.
        else -> base
    }
    Box(
        surface.semantics(mergeDescendants = true) {
            contentDescription = cellDescription(date, day != null && !day.draft, day?.draft == true, autoItems) + when (state) {
                CellState.TODAY -> ", today"
                CellState.EMPTY -> ", add a clip"
                else -> ""
            }
            if (day?.draft == true || autoItems != null) {
                customActions = listOf(CustomAccessibilityAction(if (autoItems != null) "Check day" else "Review draft") { onClick(); true })
            } else if (day != null) {
                customActions = listOf(
                    CustomAccessibilityAction("Open") { onClick(); true },
                    CustomAccessibilityAction("Replace clip") { onReplace(); true },
                    CustomAccessibilityAction("Remove clip") { onRemove(); true },
                )
            }
        }.then(if (state == CellState.FUTURE) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)),
        Alignment.Center,
    ) {
        when (state) {
            CellState.FUTURE -> SliverText(date.day.toString(), SliverType.tnum(color = SliverColors.TextDisabled))
            CellState.EMPTY, CellState.TODAY -> {
                val today = state == CellState.TODAY
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SliverText(date.day.toString(), SliverType.tnum(color = if (today) SliverColors.TextPrimary else SliverColors.TextTertiary))
                    // Owner request: a past empty day shows "+" so it reads as "tap to fill"; today shows the red record dot.
                    if (!today) SliverIcon(SliverIcons.Plus, SliverColors.TextDisabled, size = 14.dp)
                    else Box(Modifier.size(6.dp).clip(CircleShape).background(SliverColors.Record))
                }
            }
            else -> {
                val draft = state == CellState.DRAFT
                val thumb = if (day != null) rememberPhoto(day.thumb) else null
                Box(Modifier.fillMaxSize().then(if (draft) Modifier.alpha(DraftAlpha) else Modifier)) {
                    Box(Modifier.fillMaxSize().background(day!!.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3))
                    if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                if (draft || state == CellState.AUTO) {
                    Box(Modifier.fillMaxSize().dashedOutline(if (draft) SliverColors.TextDisabled else SliverColors.OnMedia))
                    if (draft) SliverText(
                        "Draft", SliverType.Caption.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption), color = SliverColors.OnMedia).overMedia(),
                        Modifier.align(Alignment.TopStart).padding(top = 4.dp, start = 6.dp),
                    )
                }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp).background(Brush.verticalGradient(listOf(Color.Transparent, SliverColors.Scrim))))
                SliverText(
                    date.day.toString(), SliverType.tnum(color = SliverColors.OnMedia).overMedia(),
                    Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
                )
                // Owner: a saved day with other items from an import shows it ("+3"), tap → Day detail → pick another.
                if (others > 0 && state != CellState.DRAFT && state != CellState.AUTO) {
                    SliverText(
                        "+$others", SliverType.tnum(color = SliverColors.OnMedia),
                        Modifier.align(Alignment.TopEnd).padding(3.dp).clip(SliverShapes.Pill).background(SliverColors.Scrim)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

private const val DraftAlpha = 0.4f

/** "3 auto-filled days to check" row above the calendar (52dp) → review sheet. */
@Composable
private fun ReviewRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(SliverShapes.R14).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.R14)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(20.dp).dashedOutline(SliverColors.TextSecondary, 5.dp))
        SliverText(label, SliverType.Body.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), Modifier.weight(1f), color = SliverColors.TextPrimary)
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary, size = 18.dp)
    }
}

@Composable
private fun ProjectMenu(
    canLeave: Boolean,
    onDismiss: () -> Unit, onRename: () -> Unit, onShape: () -> Unit, onFill: () -> Unit, onManage: () -> Unit,
) {
    app.sliver.core.ui.BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 11.dp + 48.dp
    Box(Modifier.fillMaxSize().padding(top = top, end = 12.dp), Alignment.TopEnd) {
        Column(
            Modifier.width(230.dp)
                .shadow(24.dp, SliverShapes.R18, ambientColor = Color.Black, spotColor = Color.Black) // lint-ok: elevation
                .clip(SliverShapes.R18).background(SliverColors.Surface2)
                .border(1.dp, SliverColors.LineStrong, SliverShapes.R18)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .padding(6.dp)
                .semantics { role = Role.DropdownList },
        ) {
            MenuItem(HubIcons.Rename, "Rename", SliverColors.TextPrimary, onRename)
            MenuItem(HubIcons.Shape, "Change orientation", SliverColors.TextPrimary, onShape)
            MenuItem(HubIcons.FillDays, "Fill the calendar", SliverColors.TextPrimary, onFill)
            // Owner 14 Nov: archive and delete must not be one tap away; they sit behind a muted last item.
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(SliverColors.LineHairline))
            MenuItem(SliverIcons.MoreHorizontal, "Archive or delete\u2026", SliverColors.TextSecondary, onManage, canLeave)
            if (!canLeave) SliverText(
                "Keep at least one open project.", SliverType.Caption, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = SliverColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) {
    val c = if (enabled) color else SliverColors.TextDisabled
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(SliverShapes.R12)
            .clickable(enabled = enabled, role = Role.Button, onClick = rememberTapGate(onClick)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SliverIcon(icon, c, size = 18.dp)
        SliverText(label, SliverType.Body.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), color = c)
    }
}

@Composable
private fun AddSheet(date: LocalDate, onPick: () -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SliverText(
            "${Weekdays3[date.dayOfWeek.ordinal]} · ${date.day} ${MonthNames[date.month.ordinal]} ${date.year}",
            SliverType.tnum(0.08.em, SliverColors.TextTertiary),
        )
        SliverText("Add a clip for this day", SliverType.DisplayM, Modifier.padding(bottom = 8.dp))
        ReelogButton("Choose from Photos", onPick, leading = HubIcons.Photos)
        ReelogButton("Leave it empty", onClose, kind = ButtonKind.Tertiary)
    }
}

private const val DockGlowMs = 14_000

/** v1.7 item 5: strong-glass capsule 72 dp, 16 dp from the sides, 30 dp above the nav bar. Content scrolls under it. */
private val DockCapsule = 72.dp
private val RecordDockHeight = DockCapsule + 30.dp + 24.dp

/** Record today dock. No animation on this screen (the old glow burned ~60 % CPU on the idle calendar). */
@Composable
private fun RecordDock(
    today: Day?,
    caption: String,
    backdrop: dev.chrisbanes.haze.HazeState,
    onRecord: () -> Unit,
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = today != null
    val tap = rememberTapGate(if (done) onOpenToday else onRecord)
    val title = if (done) "Today is in" else "Record today"
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 30.dp)
            .height(DockCapsule)
            .glass(GlassStyle.Strong, SliverShapes.Pill, backdrop = backdrop)
            .semantics(mergeDescendants = true) { contentDescription = title }
            .clickable(role = Role.Button, onClick = tap)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val thumb = today?.let { rememberPhoto(it.thumb) }
        Box(Modifier.size(48.dp).clip(CircleShape)) {
            if (thumb != null) {
                androidx.compose.foundation.Image(thumb, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else if (!done) {
                Box(Modifier.fillMaxSize().background(RecordCore))
            } else {
                Box(Modifier.fillMaxSize().background(SliverColors.Surface3))
            }
        }
        Column(Modifier.weight(1f)) {
            SliverText(title, ReelogType.body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            SliverText(
                if (done) "0:01 · tap to watch or replace" else caption,
                ReelogType.caption.copy(fontFeatureSettings = "tnum"), color = SliverColors.TextSecondary, maxLines = 1,
            )
        }
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextSecondary, size = 22.dp)
    }
}

/** Plain red record core (GlassKit §3 colours, no glow). */
private val RecordCore = Brush.radialGradient(
    0f to SliverColors.RecordLight, 0.55f to SliverColors.Record, 1f to SliverColors.RecordDeep,
    center = androidx.compose.ui.geometry.Offset(0.38f * 144f, 0.30f * 144f), radius = 110f,
)
