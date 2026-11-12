package app.sliver.feature.archive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import app.sliver.core.ui.rememberTapGate
import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
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
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    val projects by remember { koin.get<ProjectRepository>().projects }.collectAsState(emptyList())
    val first = project?.startDate ?: app?.firstDay ?: all.minOfOrNull { it.date } ?: today
    val memory = remember(byDate, today) { pickMemory(today, byDate.keys)?.let { m -> byDate[m.date]?.let { it to m.kind } } }

    val controller = rememberPermissionController()
    val scope = rememberCoroutineScope()
    val undo = remember { koin.get<SaveUndo>() }
    var notice by remember { mutableStateOf<String?>(null) }
    var yearView by rememberSaveable { mutableStateOf(false) }
    var shownIndex by rememberSaveable { mutableStateOf(today.toYearMonth().let { it.year * 12 + it.month - 1 }) }
    val shown = YearMonth(shownIndex / 12, shownIndex % 12 + 1)
    fun show(m: YearMonth) { shownIndex = m.year * 12 + m.month - 1 }
    var addFor by remember { mutableStateOf<LocalDate?>(null) }
    var projectsOpen by remember { mutableStateOf(false) }
    var projectAction by remember { mutableStateOf<ProjectAction?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val wide = !LocalWindowSize.current.isCompact
    var paneDay by rememberSaveable { mutableStateOf<String?>(null) }
    val pane = paneDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun openDay(date: LocalDate) {
        ArchiveSession.selected = date
        if (wide) paneDay = date.toString() else onNavigate(Route.DayDetail)
    }

    fun onCell(date: LocalDate) {
        when (cellState(date, today, byDate[date], first)) {
            CellState.FUTURE -> Unit
            CellState.TODAY -> {
                CaptureSession.forDate = null
                onNavigate(Route.Camera)
            }
            CellState.EMPTY -> addFor = date
            CellState.FILLED, CellState.BACKFILLED -> openDay(date)
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

    val reminderOff = app?.reminder?.enabled == false ||
        controller.status(PermissionKind.Notifications) != PermissionStatus.Granted
    val showNudge = app != null && !app!!.nudgeDismissed && reminderOff

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            Column(
                (if (wide) Modifier.weight(1f) else Modifier.widthIn(max = MaxContentWidth).fillMaxWidth())
                    .fillMaxHeight().navigationBarsPadding(),
            ) {
                Header(
                    project?.name.orEmpty(), clipsHeader(all.size, first, project?.format), menuOpen,
                    { projectsOpen = true }, { onNavigate(Route.Settings) }, { menuOpen = true },
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = RecordDockHeight),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        ViewRow(yearView, { yearView = it }, canGoPrev(shown, first), canGoNext(shown, today),
                            { show(shown.plusMonths(-1)) }, { show(shown.plusMonths(1)) })

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

                        if (yearView) {
                            YearView(shown.year, byDate, today, first, { ym ->
                                show(ym)
                                yearView = false
                            }, ::openDay)
                        } else {
                            val count = remember(all, shown) { all.count { it.date.year == shown.year && it.date.month.ordinal + 1 == shown.month } }
                            MonthHeading(MonthLong[shown.month - 1], "$count ${if (count == 1) "clip" else "clips"}")
                            MonthGrid(shown, byDate, today, first, ::onCell, ::replaceDay, ::removeDay)
                        }

                        memory?.let { (day, kind) -> MemoryCard(day, kind) { openDay(day.date) } }
                    }
                    if (!wide) FilmsSection(onNavigate)
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
                        ) { FilmsSection(onNavigate) }
                    }
                }
            }
        }

        RecordDock(
            done = byDate[today] != null,
            onRecord = { CaptureSession.forDate = null; onNavigate(Route.Camera) },
            onOpenToday = { openDay(today) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        ProjectsSheet(projectsOpen, { projectsOpen = false })
        ProjectsSheet(projectAction != null, { projectAction = null }, action = projectAction)
        NoticeSheet(notice) { notice = null }
        if (menuOpen) {
            val canLeave = project?.let { canLeaveActive(it, projects) } ?: false
            ProjectMenu(
                canLeave,
                onDismiss = { menuOpen = false },
                onRename = { menuOpen = false; projectAction = ProjectAction.Rename },
                onShape = { menuOpen = false; projectAction = ProjectAction.Format },
                onFill = { menuOpen = false; onNavigate(Route.FillThePast) },
                onYear = { menuOpen = false; onNavigate(Route.YearInClips) },
                onArchive = {
                    menuOpen = false
                    project?.let { p -> scope.launch { koin.get<ProjectRepository>().setArchived(p.id, true) } }
                },
                onDelete = { menuOpen = false; projectAction = ProjectAction.Delete },
            )
        }
        SliverBottomSheet(addFor != null, onDismiss = { addFor = null }) {
            val date = addFor ?: return@SliverBottomSheet
            AddSheet(date, { addFor = null; addMedia(date) }, { addFor = null })
        }
    }
}

private val hubTitle get() = SliverType.DisplayM.copy(lineHeight = 24.sp)

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
            HubButton(HubIcons.Gear, "Settings", onSettings, SliverColors.TextSecondary, 21.dp)
            HubButton(HubIcons.More, "More", onMore, SliverColors.TextSecondary, 20.dp, if (menuOpen) SliverColors.Surface3 else Color.Transparent)
        }
        SliverText(line, SliverType.tnum(11.sp, 0.06.em, SliverColors.TextTertiary), Modifier.padding(start = 20.dp, top = 2.dp), maxLines = 1)
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
private fun ViewRow(yearView: Boolean, onYear: (Boolean) -> Unit, canPrev: Boolean, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf("Month" to false, "Year" to true).forEach { (label, isYear) ->
                val on = yearView == isYear
                Column(
                    Modifier.width(IntrinsicSize.Max).semantics { selected = on; role = Role.Tab }.clickable(onClick = { onYear(isYear) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SliverText(
                        label, SliverType.Headline.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
                        Modifier.padding(vertical = 8.dp), if (on) SliverColors.TextPrimary else SliverColors.TextTertiary,
                        maxLines = 1, softWrap = false,
                    )
                    Box(Modifier.height(2.dp).fillMaxWidth().background(if (on) SliverColors.TextPrimary else Color.Transparent))
                }
            }
        }
        if (!yearView) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                NavArrow(HubIcons.Prev, "Previous month", canPrev, onPrev)
                NavArrow(HubIcons.Next, "Next month", canNext, onNext)
            }
        }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).semantics { contentDescription = label }
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
                SliverText("Want a daily nudge?", SliverType.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp))
                SliverText("One quiet reminder at a time you choose.", SliverType.Caption.copy(lineHeight = 17.sp), color = SliverColors.TextTertiary)
            }
        }
        Row(Modifier.padding(start = 34.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val set = rememberTapGate(onSet)
            Box(
                Modifier.height(36.dp).clip(SliverShapes.Pill).background(SliverColors.TextPrimary)
                    .clickable(role = Role.Button, onClick = set).padding(horizontal = 14.dp),
                Alignment.Center,
            ) { SliverText("Set reminder", SliverType.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp), color = SliverColors.OnInverse) }
            Box(
                Modifier.height(36.dp).clip(SliverShapes.Pill).border(1.dp, SliverColors.LineStrong, SliverShapes.Pill)
                    .clickable(role = Role.Button, onClick = onDismiss).padding(horizontal = 14.dp),
                Alignment.Center,
            ) { SliverText("No thanks", SliverType.BodySmall.copy(fontSize = 14.sp, lineHeight = 18.sp), color = SliverColors.TextSecondary) }
        }
    }
}

@Composable
private fun MonthHeading(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SliverText(left, SliverType.DisplayM.copy(fontSize = 27.sp, lineHeight = 27.sp), Modifier.alignByBaseline(), maxLines = 1, softWrap = false)
        Box(Modifier.weight(1f).alignByBaseline(), contentAlignment = Alignment.CenterEnd) {
            SliverText(right, SliverType.tnum(11.sp, 0.06.em, SliverColors.TextTertiary), maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun MonthGrid(
    shown: YearMonth, byDate: Map<LocalDate, Day>, today: LocalDate, start: LocalDate,
    onCell: (LocalDate) -> Unit, onReplace: (LocalDate) -> Unit, onRemove: (LocalDate) -> Unit,
) {
    val weeks = remember(shown) { monthGrid(shown.year, shown.month).filter { week -> week.any { it != null } } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WeekDays.forEach {
                SliverText(it, SliverType.tnum(10.sp, 0.sp, SliverColors.TextTertiary).copy(textAlign = TextAlign.Center), Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(58.dp)) {
                            if (date != null) DayCell(date, today, start, byDate[date], { onReplace(date) }, { onRemove(date) }) { onCell(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, start: LocalDate, day: Day?, onReplace: () -> Unit, onRemove: () -> Unit, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var ringed by remember { mutableStateOf(false) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(day?.takenAt) {
        if (day != null && ClipDrop.pending == date) {
            ringed = true
            ClipDrop.clear()
            if (!reduced) {
                scale.snapTo(0.6f)
                scale.animateTo(1f, tween(420, easing = SliverMotion.EaseEmphasized))
            }
        }
    }
    val state = cellState(date, today, day, start)
    val base = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CellShape)
    val surface = when (state) {
        CellState.FUTURE -> base
        CellState.EMPTY -> base.background(SliverColors.Surface1).border(1.dp, SliverColors.CellEmptyLine, CellShape)
        CellState.TODAY -> base.border(2.dp, SliverColors.AccentMark, CellShape)
        else -> base.then(if (ringed) Modifier.border(2.dp, SliverColors.AccentMark, CellShape) else Modifier)
    }
    Box(
        surface.semantics(mergeDescendants = true) {
            contentDescription = cellDescription(date, day != null) + if (state == CellState.TODAY) ", today" else ""
            if (day != null) {
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
            CellState.FUTURE -> SliverText(date.day.toString(), SliverType.tnum(11.sp, 0.sp, SliverColors.TextDisabled))
            CellState.EMPTY, CellState.TODAY -> {
                val tint = if (state == CellState.TODAY) SliverColors.AccentText else null
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SliverText(date.day.toString(), SliverType.tnum(11.sp, 0.sp, tint ?: SliverColors.TextTertiary))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(tint ?: SliverColors.LineStrong))
                }
            }
            else -> {
                val thumb = if (day != null) rememberPhoto(day.thumb) else null
                Box(Modifier.fillMaxSize().background(day!!.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3))
                if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp).background(Brush.verticalGradient(listOf(Color.Transparent, SliverColors.Scrim))))
                SliverText(
                    date.day.toString(), SliverType.tnum(10.sp, 0.sp, SliverColors.OnMedia).overMedia(),
                    Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
                )
            }
        }
    }
}

private fun doubleDigits(n: Int) = n.toString().padStart(2, '0')

@Composable
private fun YearView(
    year: Int, byDate: Map<LocalDate, Day>, today: LocalDate, first: LocalDate,
    onMonth: (YearMonth) -> Unit, onOpenDay: (LocalDate) -> Unit,
) {
    val inYear = remember(byDate, year) { byDate.keys.count { it.year == year } }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MonthHeading("$year", "$inYear ${if (inYear == 1) "clip" else "clips"} · ${doubleDigits(inYear / 60)}:${doubleDigits(inYear % 60)} of film")
        LightOfYear(byDate, today, first, onMonth, onOpenDay)
    }
}

@Composable
private fun MemoryCard(day: Day, kind: MemoryKind, onClick: () -> Unit) {
    val thumb = rememberPhoto(day.thumb)
    val d = day.date
    Row(
        Modifier.fillMaxWidth().height(76.dp).clip(SliverShapes.R18).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.R18).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp).clip(CellShape).background(day.color.takeIf { it != 0 }?.let(::Color) ?: SliverColors.Surface3), Alignment.Center) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            SliverIcon(HubIcons.Play, SliverColors.TextPrimary, size = 18.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SliverText(
                "${kind.label} · ${Weekdays3[d.dayOfWeek.ordinal]} ${d.day} ${MonthNames[d.month.ordinal]}",
                SliverType.tnum(11.sp, 0.06.em, SliverColors.AccentText), maxLines = 1,
            )
            SliverText("A clip you might have forgotten", SliverType.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp))
        }
    }
}

@Composable
private fun ProjectMenu(
    canLeave: Boolean,
    onDismiss: () -> Unit, onRename: () -> Unit, onShape: () -> Unit, onFill: () -> Unit, onYear: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit,
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
            MenuItem(HubIcons.Shape, "Change shape", SliverColors.TextPrimary, onShape)
            MenuItem(HubIcons.FillDays, "Fill empty days", SliverColors.TextPrimary, onFill)
            MenuItem(SliverIcons.Calendar, "Year in clips", SliverColors.TextPrimary, onYear)
            MenuItem(HubIcons.Archive, "Archive project", SliverColors.TextPrimary, onArchive, canLeave)
            MenuItem(HubIcons.Trash, "Delete project", SliverColors.Error, onDelete, canLeave)
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
        SliverText(label, SliverType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp), color = c)
    }
}

@Composable
private fun AddSheet(date: LocalDate, onPick: () -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SliverText(
            "${Weekdays3[date.dayOfWeek.ordinal]} · ${date.day} ${MonthNames[date.month.ordinal]} ${date.year}",
            SliverType.tnum(11.sp, 0.08.em, SliverColors.TextTertiary),
        )
        SliverText("Add a clip for this day", SliverType.DisplayM.copy(lineHeight = 25.sp), Modifier.padding(bottom = 8.dp))
        val pick = rememberTapGate(onPick)
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(SliverShapes.Pill)
                .background(SliverColors.Accent).clickable(role = Role.Button, onClick = pick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            SliverIcon(HubIcons.Photos, SliverColors.OnAccent, size = 18.dp)
            SliverText("Choose from Photos", SliverType.Headline.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp), color = SliverColors.OnAccent)
        }
        Box(Modifier.fillMaxWidth().height(48.dp).clickable(role = Role.Button, onClick = onClose), Alignment.Center) {
            SliverText("Leave it empty", SliverType.BodySmall.copy(fontSize = 15.sp, lineHeight = 20.sp), color = SliverColors.TextSecondary)
        }
    }
}

private val RecordDockHeight = 148.dp

/** Fixed bottom action: red "Record today", or an outlined check once today has a clip. */
@Composable
private fun RecordDock(done: Boolean, onRecord: () -> Unit, onOpenToday: () -> Unit, modifier: Modifier = Modifier) {
    val tap = rememberTapGate(if (done) onOpenToday else onRecord)
    val label = if (done) "Done for today" else "Record today"
    Column(
        modifier.fillMaxWidth().navigationBarsPadding()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to SliverColors.BgBase.copy(alpha = 0.92f), 1f to SliverColors.BgBase))
            .padding(top = 28.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(72.dp)
                .clip(CircleShape)
                .then(if (done) Modifier.border(2.dp, SliverColors.LineStrong, CircleShape) else Modifier.background(SliverColors.Record))
                .semantics { contentDescription = label }
                .clickable(role = Role.Button, onClick = tap),
            Alignment.Center,
        ) { if (done) SliverIcon(SliverIcons.Check, SliverColors.TextPrimary, size = 28.dp) }
        SliverText(label, SliverType.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = if (done) SliverColors.TextSecondary else SliverColors.TextPrimary)
    }
}
