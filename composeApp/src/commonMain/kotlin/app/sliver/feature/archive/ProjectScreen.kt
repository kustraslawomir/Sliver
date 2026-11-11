package app.sliver.feature.archive

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import app.sliver.feature.trim.SaveUndo
import app.sliver.core.theme.SliverMotion
import app.sliver.core.ui.ClipDrop
import app.sliver.core.ui.clipDropTarget
import kotlinx.coroutines.flow.first
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import app.sliver.core.ui.LocalWindowSize
import app.sliver.core.ui.MaxContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.data.settings.ReminderMode
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.monthGrid
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.EdgeState
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.data.DaySource
import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.feature.films.FilmsSection
import app.sliver.feature.projects.ProjectsSheet
import app.sliver.core.ui.rememberTapGate
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.trim.dayLabel
import app.sliver.feature.trim.rememberPhoto
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

/** Date handed to the Day detail screen until it takes a route argument. */
object ArchiveSession {
    var selected: LocalDate? = null
}

private val WeekDays = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun ProjectScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val today = remember { todayLocal() }
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)
    val byDate = remember(all) { all.associateBy { it.date } }
    val first = app?.firstDay ?: all.minOfOrNull { it.date } ?: today
    var seen by remember { mutableStateOf<Set<LocalDate>?>(null) }
    LaunchedEffect(app != null, byDate.isNotEmpty()) { if (app != null && seen == null) seen = byDate.keys }
    val fresh = seen?.let { byDate.keys - it } ?: emptySet()
    val memory = remember(byDate, today) { pickMemory(today, byDate.keys) }

    val controller = rememberPermissionController()
    val scope = rememberCoroutineScope()
    val undo = remember { koin.get<SaveUndo>() }
    var yearView by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(today.toYearMonth()) }
    var addFor by remember { mutableStateOf<LocalDate?>(null) }
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    var projectsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val wide = !LocalWindowSize.current.isCompact
    var paneDay by rememberSaveable { mutableStateOf<String?>(null) }
    val pane = paneDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun openDay(date: LocalDate) {
        ArchiveSession.selected = date
        if (wide) paneDay = date.toString() else onNavigate(Route.DayDetail)
    }

    fun onCell(date: LocalDate) {
        when (cellState(date, today, byDate[date])) {
            CellState.FUTURE -> Unit
            CellState.TODAY -> {
                CaptureSession.forDate = null
                onNavigate(Route.Camera)
            }
            CellState.EMPTY -> addFor = date
            CellState.FILLED, CellState.BACKFILLED -> openDay(date)
        }
    }

    fun replaceDay(date: LocalDate) {
        CaptureSession.forDate = date
        onNavigate(if (date == today) Route.Camera else Route.GalleryPicker)
    }

    fun removeDay(date: LocalDate) {
        val d = byDate[date] ?: return
        scope.launch { undo.remove(d, dayLabel(date, today)) }
    }

    fun fromGallery(date: LocalDate) {
        CaptureSession.forDate = date
        addFor = null
        onNavigate(Route.GalleryPicker)
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
        Column(
            (if (wide) Modifier.weight(1f) else Modifier.widthIn(max = MaxContentWidth).fillMaxWidth())
                .fillMaxHeight().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4)
                .padding(bottom = SliverSpace.S6),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton44(onBack, "Back to camera") {
                    SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary)
                }
                Column(
                    Modifier.weight(1f).clip(SliverShapes.S).clickable(onClick = rememberTapGate { projectsOpen = true }),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SliverText(project?.name ?: "", SliverType.DisplayM, Modifier.weight(1f, fill = false), maxLines = 1)
                        SliverIcon(SliverIcons.ChevronDown, SliverColors.TextSecondary)
                    }
                    SliverText(secondsHeader(all.size, first, project?.format), SliverType.MonoS, color = SliverColors.TextSecondary)
                }
                IconButton44({ onNavigate(Route.Settings) }, "Settings") {
                    SliverIcon(SliverIcons.Settings, SliverColors.TextPrimary)
                }
                IconButton44({ menuOpen = true }, "More") {
                    SliverIcon(SliverIcons.More, SliverColors.TextPrimary)
                }
            }
            Segmented(listOf("MONTH", "YEAR"), if (yearView) 1 else 0, { yearView = it == 1 })

            if (all.isEmpty() && app != null) {
                EdgeState(
                    "Your year starts today", "Record one clip a day. Your archive fills in here.",
                    Modifier.fillMaxWidth(), "Record today's clip",
                ) { CaptureSession.forDate = null; onNavigate(Route.Camera) }
            }

            if (all.size >= REMINDER_CARD_CLIPS && app?.reminder?.mode != ReminderMode.OFF &&
                controller.status(PermissionKind.Notifications) == PermissionStatus.NotAsked
            ) {
                ReminderCard(
                    onSet = { controller.request(PermissionKind.Notifications) },
                    onDismiss = { scope.launch { settings.update { it.copy(reminder = it.reminder.copy(mode = ReminderMode.OFF)) } } },
                )
            }

            memory?.let { m ->
                byDate[m.date]?.let { MemoryCard(it, m.kind, today) { openDay(m.date) } }
            }

            if (yearView) {
                YearView(shown.year, byDate, today, first) { month ->
                    shown = YearMonth(shown.year, month)
                    yearView = false
                }
            } else {
                MonthHeader(
                    shown, canGoPrev(shown, first), canGoNext(shown, today),
                    { shown = shown.plusMonths(-1) }, { shown = shown.plusMonths(1) },
                )
                MonthGrid(shown, byDate, today, fresh, ::onCell, ::replaceDay, ::removeDay)
            }
            if (!wide) FilmsSection(onNavigate)
        }
        if (wide) {
            Box(Modifier.weight(1.3f).fillMaxHeight()) {
                if (pane != null) {
                    key(pane) { DayDetailScreen(onNavigate, onBack = { paneDay = null }) }
                } else {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
                    ) { FilmsSection(onNavigate) }
                }
            }
        }
        }

        ProjectsSheet(projectsOpen, { projectsOpen = false })
        SliverBottomSheet(menuOpen, onDismiss = { menuOpen = false }) {
            val tap = rememberTapGate { menuOpen = false; onNavigate(Route.FillThePast) }
            SliverText(
                "Fill empty days", SliverType.Body,
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = tap).padding(vertical = SliverSpace.S3),
            )
        }
        SliverBottomSheet(addFor != null, onDismiss = { addFor = null }) {
            val date = addFor ?: return@SliverBottomSheet
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText(dayLabel(date, today), SliverType.DisplayM)
                SliverText("Nothing saved for this day yet.", SliverType.BodySmall)
                PrimaryButton("Video from gallery", { fromGallery(date) }, Modifier.fillMaxWidth())
                SecondaryButton("Photo", { fromGallery(date) }, Modifier.fillMaxWidth())
                GhostButton("Leave it open", { addFor = null }, Modifier.fillMaxWidth())
            }
        }
    }
}

private const val REMINDER_CARD_CLIPS = 3

@Composable
private fun ReminderCard(onSet: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(SliverShapes.M).background(SliverColors.Surface1).padding(SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        SliverText("Want a daily nudge?", SliverType.Headline)
        Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            SecondaryButton("Set reminder", onSet, Modifier.weight(1f))
            GhostButton("No thanks", onDismiss)
        }
    }
}

@Composable
private fun MonthHeader(
    shown: YearMonth,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton44(onPrev, "Previous month") {
            SliverIcon(SliverIcons.ChevronLeft, if (canPrev) SliverColors.TextPrimary else SliverColors.TextDisabled)
        }
        SliverText(
            "${MonthLong[shown.month - 1]} ${shown.year}", SliverType.DisplayM,
            Modifier.weight(1f).padding(horizontal = SliverSpace.S2),
        )
        IconButton44(onNext, "Next month") {
            SliverIcon(SliverIcons.ChevronRight, if (canNext) SliverColors.TextPrimary else SliverColors.TextDisabled)
        }
    }
}

@Composable
private fun MonthGrid(
    shown: YearMonth, byDate: Map<LocalDate, Day>, today: LocalDate, fresh: Set<LocalDate>,
    onCell: (LocalDate) -> Unit, onReplace: (LocalDate) -> Unit, onRemove: (LocalDate) -> Unit,
) {
    val weeks = remember(shown) { monthGrid(shown.year, shown.month).filter { week -> week.any { it != null } } }
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S1 + 1.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1 + 1.dp)) {
            WeekDays.forEach {
                SliverText(it, SliverType.MonoS, Modifier.weight(1f), SliverColors.TextTertiary)
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1 + 1.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).height(60.dp)) {
                        if (date != null) DayCell(date, today, byDate[date], date in fresh, { onReplace(date) }, { onRemove(date) }) { onCell(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, day: Day?, isNew: Boolean, onReplace: () -> Unit, onRemove: () -> Unit, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isNew) {
        if (isNew) {
            scale.snapTo(0f)
            snapshotFlow { ClipDrop.pending != date }.first { it }
            scale.animateTo(1f, SliverMotion.springDrop())
        }
    }
    val state = cellState(date, today, day)
    val shape = SliverShapes.S
    val base = Modifier.fillMaxSize().clip(shape).background(
        if (state == CellState.FUTURE) Color.Transparent else SliverColors.Surface1,
    )
    val ring = if (state == CellState.TODAY) base.border(2.dp, SliverColors.Accent, shape) else base
    Box(
        ring.clipDropTarget(date).graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = scale.value }.semantics(mergeDescendants = true) {
                contentDescription = cellDescription(date, day != null) + if (state == CellState.TODAY) ", today" else ""
                if (day != null) {
                    customActions = listOf(
                        CustomAccessibilityAction("Open") { onClick(); true },
                        CustomAccessibilityAction("Replace clip") { onReplace(); true },
                        CustomAccessibilityAction("Remove clip") { onRemove(); true },
                    )
                }
            }
            .then(if (state == CellState.FUTURE) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)),
    ) {
        val thumb = if (day != null) rememberPhoto(day.thumb) else null
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (day != null) Box(Modifier.fillMaxSize().background(Color(day.color)))
        if (state == CellState.EMPTY) {
            Box(Modifier.align(Alignment.Center).size(4.dp).clip(SliverShapes.Pill).background(SliverColors.LineStrong))
        }
        SliverText(
            date.day.toString(), SliverType.MonoS,
            Modifier.align(Alignment.BottomStart).padding(SliverSpace.S1 + 2.dp),
            if (state == CellState.FUTURE) SliverColors.TextDisabled else SliverColors.TextPrimary,
        )
        if (state == CellState.TODAY) {
            Box(Modifier.align(Alignment.TopStart).padding(SliverSpace.S2).size(6.dp).clip(SliverShapes.Pill).background(SliverColors.AccentCore))
        }
        if (state == CellState.BACKFILLED) {
            SliverIcon(SliverIcons.ImagePlus, SliverColors.AccentCore, Modifier.align(Alignment.TopEnd).padding(SliverSpace.S1), size = 12.dp)
        }
    }
}

@Composable
private fun YearView(year: Int, byDate: Map<LocalDate, Day>, today: LocalDate, first: LocalDate, onMonth: (Int) -> Unit) {
    val rows = remember(year, byDate) { yearColors(year, byDate) }
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
        rows.forEachIndexed { m, row ->
            val inRange = YearMonth(year, m + 1) in first.toYearMonth()..today.toYearMonth()
            Row(
                Modifier.fillMaxWidth().clip(SliverShapes.Xs).clickable(enabled = inRange) { onMonth(m + 1) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliverText(MonthNames[m], SliverType.MonoS, Modifier.width(36.dp), SliverColors.TextTertiary)
                Canvas(Modifier.weight(1f).height(14.dp)) {
                    val step = size.width / 31f
                    val cell = step - 1.5f
                    row.forEachIndexed { d, color ->
                        if (color == null) return@forEachIndexed
                        drawRect(
                            if (color == 0) SliverColors.Surface1 else Color(color),
                            Offset(d * step, 0f),
                            Size(cell, size.height),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(day: Day, kind: MemoryKind, today: LocalDate, onClick: () -> Unit) {
    val thumb = rememberPhoto(day.thumb)
    Row(
        Modifier.fillMaxWidth().clip(SliverShapes.L).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.L).clickable(onClick = onClick)
            .padding(SliverSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Box(Modifier.size(width = 64.dp, height = 40.dp).clip(SliverShapes.S).background(Color(day.color))) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column {
            SliverText(kind.label.uppercase(), SliverType.MonoS, color = SliverColors.Accent)
            SliverText(dayLabel(day.date, today) + " " + MonthLong[day.date.month.ordinal].take(3), SliverType.BodySmall)
        }
    }
}
