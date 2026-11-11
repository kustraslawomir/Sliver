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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import app.sliver.core.designsystem.Glow
import app.sliver.core.designsystem.glow
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
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.films.FilmsSection
import app.sliver.feature.films.mono
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
private val Weekdays3 = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val CellShape get() = SliverShapes.S
private val YearEmpty = Color(0xFF1C1C20)
private val CellEmptyLine = Color(0xFF1F1F22)

@Composable
fun ProjectScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
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
    val memoryDay = remember(byDate, today) { byDate[today.minus(DatePeriod(months = 1))] }

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

    val reminderOff = app?.reminder?.mode == ReminderMode.OFF ||
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
                    onBack, { projectsOpen = true }, { onNavigate(Route.Settings) }, { menuOpen = true },
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 48.dp),
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
                                onSet = { onNavigate(Route.Notifications) },
                                onDismiss = { scope.launch { settings.update { it.copy(nudgeDismissed = true) } } },
                            )
                        }

                        if (yearView) {
                            YearView(shown.year, byDate, today, first, all.size) { month ->
                                show(YearMonth(shown.year, month))
                                yearView = false
                            }
                        } else {
                            val count = remember(all, shown) { all.count { it.date.year == shown.year && it.date.month.ordinal + 1 == shown.month } }
                            MonthHeading(MonthLong[shown.month - 1], "$count ${if (count == 1) "CLIP" else "CLIPS"}")
                            MonthGrid(shown, byDate, today, first, ::onCell, ::replaceDay, ::removeDay)
                        }

                        memoryDay?.let { MemoryCard(it, today) { openDay(it.date) } }
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
    onBack: () -> Unit, onSwitch: () -> Unit, onSettings: () -> Unit, onMore: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(SliverColors.BgBase).statusBarsPadding().padding(top = 11.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HubButton(HubIcons.Back, "Back to camera", onBack, SliverColors.TextPrimary, 22.dp)
            Row(
                Modifier.weight(1f).height(44.dp).clickable(role = Role.Button, onClick = rememberTapGate(onSwitch))
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
        SliverText(line, mono(11.sp, 0.06.em, SliverColors.TextTertiary), Modifier.padding(start = 20.dp, top = 2.dp), maxLines = 1)
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
        Row(
            Modifier.clip(SliverShapes.Pill).background(SliverColors.Surface1)
                .border(1.dp, SliverColors.LineHairline, SliverShapes.Pill).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("Month" to false, "Year" to true).forEach { (label, isYear) ->
                val on = yearView == isYear
                Box(
                    Modifier.height(32.dp).clip(RoundedCornerShape16)
                        .background(if (on) SliverColors.TextPrimary else Color.Transparent)
                        .semantics { selected = on; role = Role.Tab }
                        .clickable(onClick = { onYear(isYear) }).padding(horizontal = 16.dp),
                    Alignment.Center,
                ) {
                    SliverText(
                        label, SliverType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
                        color = if (on) SliverColors.OnAccent else SliverColors.TextTertiary,
                    )
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

private val RoundedCornerShape16 = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

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
            SliverIcon(HubIcons.Bell, SliverColors.Accent)
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
            ) { SliverText("Set reminder", SliverType.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp), color = SliverColors.OnAccent) }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SliverText(left, SliverType.DisplayM.copy(fontSize = 27.sp, lineHeight = 27.sp), Modifier.alignByBaseline())
        SliverText(right, mono(11.sp, 0.06.em, SliverColors.TextTertiary), Modifier.alignByBaseline())
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
                SliverText(it, mono(10.sp, 0.sp, SliverColors.TextTertiary).copy(textAlign = TextAlign.Center), Modifier.weight(1f))
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
        CellState.EMPTY -> base.background(SliverColors.Surface1).border(1.dp, CellEmptyLine, CellShape)
        CellState.TODAY -> base.border(2.dp, SliverColors.Accent, CellShape)
        else -> base.then(if (ringed) Modifier.border(2.dp, SliverColors.Accent, CellShape) else Modifier)
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
            CellState.FUTURE -> SliverText(date.day.toString(), mono(11.sp, 0.sp, SliverColors.TextDisabled))
            CellState.EMPTY, CellState.TODAY -> {
                val tint = if (state == CellState.TODAY) SliverColors.Accent else null
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SliverText(date.day.toString(), mono(11.sp, 0.sp, tint ?: SliverColors.TextTertiary))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(tint ?: SliverColors.LineStrong))
                }
            }
            else -> {
                val thumb = if (day != null) rememberPhoto(day.thumb) else null
                Box(Modifier.fillMaxSize().background(Color(day!!.color)))
                if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                SliverText(
                    date.day.toString(), mono(10.sp, 0.sp),
                    Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
                )
            }
        }
    }
}

private fun doubleDigits(n: Int) = n.toString().padStart(2, '0')

@Composable
private fun YearView(year: Int, byDate: Map<LocalDate, Day>, today: LocalDate, first: LocalDate, total: Int, onMonth: (Int) -> Unit) {
    val rows = remember(year, byDate) { yearColors(year, byDate) }
    val inYear = remember(byDate, year) { byDate.keys.count { it.year == year } }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MonthHeading("$year", "$inYear ${if (inYear == 1) "CLIP" else "CLIPS"} · ${doubleDigits(inYear / 60)}:${doubleDigits(inYear % 60)} OF FILM")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEachIndexed { m, row ->
                val inRange = YearMonth(year, m + 1) in first.toYearMonth()..today.toYearMonth()
                Row(
                    Modifier.fillMaxWidth().height(16.dp).clickable(enabled = inRange, role = Role.Button) { onMonth(m + 1) }
                        .semantics { contentDescription = MonthLong[m] },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SliverText(MonthNames[m], mono(10.sp, 0.sp, if (inRange) SliverColors.TextSecondary else SliverColors.TextDisabled), Modifier.width(32.dp))
                    Canvas(Modifier.weight(1f).height(16.dp)) {
                        val w = 7.8.dp.toPx()
                        val step = w + 2.3.dp.toPx()
                        val r = CornerRadius(2.dp.toPx())
                        row.forEachIndexed { d, color ->
                            if (color == null) return@forEachIndexed
                            val date = LocalDate(year, m + 1, d + 1)
                            val x = d * step
                            val box = Size(w, size.height)
                            when {
                                color != 0 -> drawRoundRect(Color(color), Offset(x, 0f), box, r)
                                date > today -> {
                                    val s = 1.dp.toPx()
                                    drawRoundRect(SliverColors.LineHairline, Offset(x + s / 2, s / 2), Size(w - s, size.height - s), r, style = androidx.compose.ui.graphics.drawscope.Stroke(s))
                                }
                                date == today -> drawRoundRect(SliverColors.Accent, Offset(x, 0f), box, r)
                                date < first -> drawRoundRect(SliverColors.Surface1, Offset(x, 0f), box, r)
                                else -> drawRoundRect(YearEmpty, Offset(x, 0f), box, r)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(day: Day, today: LocalDate, onClick: () -> Unit) {
    val thumb = rememberPhoto(day.thumb)
    val d = day.date
    Row(
        Modifier.fillMaxWidth().height(76.dp).clip(SliverShapes.R18).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.R18).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp).clip(CellShape).background(Color(day.color)), Alignment.Center) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            SliverIcon(HubIcons.Play, SliverColors.TextPrimary, size = 18.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SliverText(
                "ONE MONTH AGO · ${Weekdays3[d.dayOfWeek.ordinal]} ${d.day} ${MonthNames[d.month.ordinal]}",
                mono(11.sp, 0.06.em, SliverColors.Accent), maxLines = 1,
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
                .then(app.sliver.core.theme.SliverGlow.sheet(SliverShapes.R18))
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
            mono(11.sp, 0.08.em, SliverColors.TextTertiary),
        )
        SliverText("Add a clip for this day", SliverType.DisplayM.copy(lineHeight = 25.sp), Modifier.padding(bottom = 8.dp))
        val pick = rememberTapGate(onPick)
        Row(
            Modifier.fillMaxWidth().height(56.dp).glow(SliverShapes.Pill, *Glow.cta).clip(SliverShapes.Pill)
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
