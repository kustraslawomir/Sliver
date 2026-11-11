package app.sliver.feature.camera

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.height
import app.sliver.core.ui.Chip
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.data.FilmFormat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionGate
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.theme.SliverShapes
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.rememberTapGate
import app.sliver.feature.archive.ArchiveSession
import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.feature.trim.rememberPhoto
import app.sliver.core.ui.Segmented
import app.sliver.data.repo.ProjectRepository
import app.sliver.feature.projects.ProjectsSheet
import app.sliver.data.files.FileStore
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.CameraSettings
import app.sliver.data.settings.Lens
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.sliver.core.ui.StorageFullOverlay
import app.sliver.core.ui.isStorageLow
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

@Composable
fun CameraScreen(onNavigate: (Route) -> Unit) {
    PermissionGate(
        PermissionKind.CameraMic,
        onDeniedAlternative = { onNavigate(Route.GalleryPicker) },
    ) {
        CameraContent(onNavigate)
    }
}

@Composable
private fun CameraContent(onNavigate: (Route) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val repo = remember { koin.get<SettingsRepository>() }
    val files = remember { koin.get<FileStore>() }
    val saved by remember { repo.settings }.collectAsState(null)
    val cam = saved?.camera ?: return Box(Modifier.fillMaxSize().background(SliverColors.BgVoid))

    val engine = rememberCameraEngine()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val thumb = rememberLatestGalleryThumb()
    val days = remember { koin.get<DayRepository>() }
    val today = remember { todayLocal() }
    val todayDay by remember { days.observeDay(today) }.collectAsState(null)
    var replacing by remember { mutableStateOf(false) }
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    val format = project?.format ?: saved?.newFilmFormat ?: FilmFormat.CINEMA
    var projectsOpen by remember { mutableStateOf(false) }
    val rotation = rememberDeviceRotation()
    val iconRot = iconRotation(rotation)
    val bandActive = format == FilmFormat.CINEMA && isUpright(rotation)
    var turnHint by remember { mutableStateOf(false) }
    LaunchedEffect(bandActive) {
        if (bandActive && !TurnHintSession.shown) {
            TurnHintSession.shown = true
            turnHint = true
            delay(TURN_HINT_MS)
            turnHint = false
        }
    }

    var ready by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var timerSec by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    fun update(change: (CameraSettings) -> CameraSettings) {
        scope.launch { repo.update { it.copy(camera = change(it.camera)) } }
    }

    LaunchedEffect(recording) {
        val start = Clock.System.now().toEpochMilliseconds()
        while (recording) {
            elapsedMs = Clock.System.now().toEpochMilliseconds() - start
            delay(100)
        }
    }

    fun finish(result: Result<String>, route: Route) {
        recording = false
        holding = false
        result.onSuccess {
            CaptureSession.path = it
            CaptureSession.source = app.sliver.data.DaySource.CAMERA
            CaptureSession.forDate = null
            onNavigate(route)
        }
    }

    var storageFull by remember { mutableStateOf(false) }
    fun storageOk(): Boolean = !isStorageLow(files.freeSpaceBytes()).also { storageFull = it }

    fun startRecording() {
        if (!storageOk()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        elapsedMs = 0
        recording = true
        engine.startRecording(files.newTmpPath(Clock.System.now().toEpochMilliseconds())) { finish(it, Route.Trim) }
    }

    fun stopRecording() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        engine.stopRecording()
    }

    fun capturePhoto() {
        if (!storageOk()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val path = files.newTmpPath(Clock.System.now().toEpochMilliseconds()).removeSuffix(".mp4") + ".jpg"
        engine.takePhoto(path) { finish(it, Route.PhotoTrim) }
    }

    fun withSelfTimer(action: () -> Unit) {
        if (timerSec == 0) return action()
        countdownJob = scope.launch {
            for (s in timerSec downTo 1) {
                countdown = s
                delay(1000)
            }
            countdown = null
            action()
        }
    }

    fun onTap() {
        when {
            countdown != null -> {
                countdownJob?.cancel()
                countdown = null
            }
            recording -> stopRecording()
            cam.mode == CameraMode.PHOTO -> withSelfTimer(::capturePhoto)
            else -> withSelfTimer(::startRecording)
        }
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 168.dp).clip(RoundedCornerShape(24.dp))) {
            CameraPreview(engine, cam.mode, cam.lens, cam.flash, Modifier.fillMaxSize()) { ready = true }
            if (bandActive) CinemaBandDim()
            if (cam.grid) GridOverlay()
            countdown?.let {
                SliverText(it.toString(), SliverType.DisplayXL, Modifier.align(Alignment.Center))
            }
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ControlButton(SliverIcons.Grid3x3, null, "Grid", cam.grid, iconRot) { update { it.copy(grid = !it.grid) } }
            if (recording) {
                SliverText(
                    formatTimer(elapsedMs), SliverType.MonoL,
                    Modifier.clip(CircleShape).background(SliverColors.BgVoid.copy(alpha = 0.5f))
                        .padding(horizontal = SliverSpace.S3, vertical = SliverSpace.S1),
                    SliverColors.Accent,
                )
            }
            ControlButton(SliverIcons.Timer, if (timerSec == 0) null else "${timerSec}s", "Self-timer", timerSec != 0, iconRot) {
                if (!recording) timerSec = nextSelfTimer(timerSec)
            }
            ControlButton(if (cam.flash) SliverIcons.Zap else SliverIcons.ZapOff, null, "Flash", cam.flash, iconRot) { update { it.copy(flash = !it.flash) } }
            ControlButton(SliverIcons.SwitchCamera, null, "Flip camera", false, iconRot) {
                if (!recording) update { it.copy(lens = if (it.lens == Lens.BACK) Lens.FRONT else Lens.BACK) }
            }
        }

        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        ) {
            if (!recording) {
                Chip(
                    "${project?.name ?: "My ${today.year}"} · ${format.label()}", selected = false,
                    onClick = { projectsOpen = true }, trailing = SliverIcons.ChevronDown,
                )
            }
            if (turnHint && bandActive) {
                SliverText(
                    "Turn sideways for the full frame", SliverType.BodySmall,
                    Modifier.clip(SliverShapes.Pill).background(SliverColors.Surface2)
                        .padding(horizontal = SliverSpace.S4, vertical = SliverSpace.S2),
                    SliverColors.TextPrimary,
                )
            }
            todayDay?.takeIf { !replacing && !recording }?.let { day ->
                TodayCard(day, { replacing = true }, { ArchiveSession.selected = today; onNavigate(Route.DayDetail) })
            }
        }

        ProjectsSheet(projectsOpen, { projectsOpen = false })

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = SliverSpace.S4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
        ) {
            Segmented(
                listOf("PHOTO", "VIDEO"),
                if (cam.mode == CameraMode.PHOTO) 0 else 1,
                { if (!recording) update { s -> s.copy(mode = if (it == 0) CameraMode.PHOTO else CameraMode.VIDEO) } },
                Modifier.width(200.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GalleryThumb(thumb) { onNavigate(Route.GalleryPicker) }
                RecordButton(
                    photo = cam.mode == CameraMode.PHOTO,
                    recording = recording,
                    holding = holding,
                    enabled = ready,
                    onTap = ::onTap,
                    onHoldStart = {
                        if (cam.mode == CameraMode.VIDEO && !recording && countdown == null) {
                            holding = true
                            startRecording()
                        }
                    },
                    onHoldEnd = { if (recording) stopRecording() },
                )
                if (recording) Box(Modifier.size(52.dp)) else ProjectButton { onNavigate(Route.Project) }
            }
        }
        if (storageFull) StorageFullOverlay({ storageFull = false })
    }
}

@Composable
private fun TodayCard(day: Day, onReplace: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val thumb = rememberPhoto(day.thumb)
    Row(
        modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth()
            .clip(SliverShapes.M).background(SliverColors.Surface1.copy(alpha = 0.92f))
            .border(1.dp, SliverColors.LineHairline, SliverShapes.M)
            .clickable(onClick = rememberTapGate(onOpen))
            .padding(SliverSpace.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Box(Modifier.size(44.dp).clip(SliverShapes.S).background(SliverColors.Surface1)) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                SliverIcon(SliverIcons.Check, SliverColors.TextPrimary, size = 16.dp)
                SliverText("Today's clip is in", SliverType.Headline)
            }
            SliverText(todayCardDetail(day.look.preset, day.takenAt), SliverType.MonoS)
        }
        GhostButton("Replace", onReplace)
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    value: String?,
    description: String,
    active: Boolean,
    rotation: Float,
    onClick: () -> Unit,
) {
    IconButton44(onClick, description, overCamera = true) {
        val tint = if (active) SliverColors.Accent else SliverColors.TextPrimary
        if (value != null) SliverText(value, SliverType.MonoS, Modifier.rotate(rotation), tint)
        else SliverIcon(icon, tint, Modifier.rotate(rotation))
    }
}

@Composable
private fun CinemaBandDim() {
    Canvas(Modifier.fillMaxSize()) {
        val band = cinemaBandFraction(size.width, size.height) * size.height
        val edge = (size.height - band) / 2f
        val dim = Color.Black.copy(alpha = 0.55f)
        drawRect(dim, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, edge))
        drawRect(dim, Offset(0f, size.height - edge), androidx.compose.ui.geometry.Size(size.width, edge))
    }
}

@Composable
private fun GalleryThumb(thumb: androidx.compose.ui.graphics.ImageBitmap?, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, RoundedCornerShape(10.dp))
            .semantics { contentDescription = "Gallery"; role = Role.Button }
            .clickable(onClick = onClick),
    ) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ProjectButton(onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, RoundedCornerShape(10.dp))
            .semantics { contentDescription = "Project calendar"; role = Role.Button }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SliverIcon(SliverIcons.Calendar, SliverColors.TextSecondary)
    }
}

@Composable
private fun RecordButton(
    photo: Boolean,
    recording: Boolean,
    holding: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val ring = if (recording) SliverColors.Accent else SliverColors.TextPrimary
    val scale = if (holding) 1.2f else 1f
    Box(
        Modifier.size(80.dp).scale(scale)
            .semantics { contentDescription = if (recording) "Stop" else "Record"; role = Role.Button }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    coroutineScope {
                        var held = false
                        val job = launch {
                            delay(HOLD_THRESHOLD_MS)
                            held = true
                            onHoldStart()
                        }
                        val released = tryAwaitRelease()
                        job.cancel()
                        when {
                            held -> onHoldEnd()
                            released -> onTap()
                        }
                    }
                })
            }
            .border(3.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (recording) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(SliverColors.Accent))
        } else {
            Box(Modifier.size(62.dp).clip(CircleShape).background(if (photo) SliverColors.TextPrimary else SliverColors.Accent))
        }
    }
}

@Composable
private fun GridOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = size.width * i / 3
            val y = size.height * i / 3
            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
    }
}
