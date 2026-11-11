package app.sliver.feature.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.motionSpec
import app.sliver.core.ui.clipDropTarget
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.LookPreset
import app.sliver.data.info
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
import androidx.compose.runtime.SideEffect
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
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.rememberTapGate
import app.sliver.feature.archive.ArchiveSession
import app.sliver.data.repo.DayRepository
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
import app.sliver.core.ui.LocalWindowSize
import app.sliver.core.ui.MaxContentWidth
import androidx.compose.foundation.layout.widthIn
import app.sliver.core.ui.isStorageLow
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

private val SideControlsWidth = 280.dp

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
    val days = remember { koin.get<DayRepository>() }
    val today = remember { todayLocal() }
    val todayDay by remember { days.observeDay(today) }.collectAsState(null)
    var filterOpen by remember { mutableStateOf(false) }
    var flipTurns by remember { mutableIntStateOf(0) }
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    val format = project?.format ?: saved?.newFilmFormat ?: FilmFormat.CINEMA
    var projectsOpen by remember { mutableStateOf(false) }
    val rotation = rememberDeviceRotation()
    SideEffect { engine.deviceDegrees = rotation }
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
        if (saved?.coachRecordSeen == false) scope.launch { repo.update { it.copy(coachRecordSeen = true) } }
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

    VolumeKeyEffect(saved?.volumeKeyRecords == true && ready) { onTap() }

    val side = LocalWindowSize.current.sideBySide
    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
        Box(
            Modifier.fillMaxSize().statusBarsPadding()
                .padding(bottom = if (side) 0.dp else 248.dp, end = if (side) SideControlsWidth else 0.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            CameraPreview(engine, cam.mode, cam.lens, cam.flash, Modifier.fillMaxSize()) { ready = true }
            if (bandActive) CinemaBandDim()
            if (cam.grid) GridOverlay()
            countdown?.let {
                SliverText(it.toString(), SliverType.DisplayXL, Modifier.align(Alignment.Center))
            }
        }

        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().widthIn(max = MaxContentWidth)
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3)
                .then(if (side) Modifier.padding(end = SideControlsWidth) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (recording) {
                    SliverText(
                        formatTimer(elapsedMs), SliverType.MonoL,
                        Modifier.clip(CircleShape).background(SliverColors.BgVoid.copy(alpha = 0.5f))
                            .padding(horizontal = SliverSpace.S3, vertical = SliverSpace.S1),
                        SliverColors.Accent,
                    )
                } else {
                    Chip(
                        project?.name ?: "My ${today.year}", selected = false,
                        onClick = { projectsOpen = true }, trailing = SliverIcons.ChevronDown,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.weight(1f))
                    val hasToday = todayDay != null
                    Chip(
                        if (hasToday) "TODAY \u2713" else "TODAY", selected = hasToday,
                        modifier = Modifier.clipDropTarget(today),
                        onClick = {
                            if (hasToday) {
                                ArchiveSession.selected = today
                                onNavigate(Route.DayDetail)
                            } else {
                                onNavigate(Route.Project)
                            }
                        },
                    )
                }
            }
            if (saved?.coachRecordSeen == false && !recording) {
                SliverText(
                    "Tap to record. Any length \u2014 you'll pick the best part next.", SliverType.BodySmall,
                    Modifier.clip(SliverShapes.Pill).background(SliverColors.Surface2)
                        .padding(horizontal = SliverSpace.S4, vertical = SliverSpace.S2),
                    SliverColors.TextPrimary,
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
        }

        ProjectsSheet(projectsOpen, { projectsOpen = false })

        Column(
            (if (side) Modifier.align(Alignment.CenterEnd).width(SideControlsWidth).statusBarsPadding()
            else Modifier.align(Alignment.BottomCenter).widthIn(max = MaxContentWidth).fillMaxWidth())
                .navigationBarsPadding().padding(bottom = SliverSpace.S4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3, if (side) Alignment.CenterVertically else Alignment.Top),
        ) {
            if (filterOpen && !recording) {
                LazyRow(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = SliverSpace.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
                ) {
                    items(LookPreset.entries) { look ->
                        Chip(look.info.label, selected = look == cam.look, onClick = { update { it.copy(look = look) } })
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolButton(SliverIcons.Grid3x3, "Grid", cam.grid, iconRot) { update { it.copy(grid = !it.grid) } }
                ToolButton(SliverIcons.Timer, if (timerSec == 0) "Timer" else "${timerSec}s", timerSec != 0, iconRot) {
                    if (!recording) timerSec = nextSelfTimer(timerSec)
                }
                ToolButton(if (cam.flash) SliverIcons.Zap else SliverIcons.ZapOff, "Light", cam.flash, iconRot) {
                    update { it.copy(flash = !it.flash) }
                }
                ToolButton(SliverIcons.SlidersHorizontal, "Filter", filterOpen || cam.look != LookPreset.NATURAL, iconRot) {
                    filterOpen = !filterOpen
                }
            }
            Segmented(
                listOf("PHOTO", "VIDEO"),
                if (cam.mode == CameraMode.PHOTO) 0 else 1,
                { if (!recording) update { s -> s.copy(mode = if (it == 0) CameraMode.PHOTO else CameraMode.VIDEO) } },
                Modifier.width(168.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (recording) Box(Modifier.size(64.dp)) else AddMediaButton(iconRot) { onNavigate(Route.GalleryPicker) }
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
                if (recording) {
                    Box(Modifier.size(64.dp))
                } else {
                    val turn by animateFloatAsState(flipTurns * 180f, tween(240), label = "flip")
                    IconButton44(
                        {
                            flipTurns++
                            update { it.copy(lens = if (it.lens == Lens.BACK) Lens.FRONT else Lens.BACK) }
                        },
                        "Switch camera", Modifier.size(64.dp),
                    ) {
                        SliverIcon(SliverIcons.SwitchCamera, SliverColors.TextPrimary, Modifier.rotate(turn))
                    }
                }
            }
        }
        if (storageFull) StorageFullOverlay({ storageFull = false })
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, active: Boolean, rotation: Float, onClick: () -> Unit) {
    val tint = if (active) SliverColors.Accent else SliverColors.TextPrimary
    val turn by animateFloatAsState(rotation, tween(240), label = "tool-upright")
    Column(
        Modifier.width(64.dp).clip(SliverShapes.S).clickable(role = Role.Button, onClick = rememberTapGate(onClick))
            .semantics { contentDescription = label }.padding(vertical = SliverSpace.S1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(44.dp), Alignment.Center) { SliverIcon(icon, tint, Modifier.rotate(turn)) }
        SliverText(label, SliverType.Caption, color = tint, maxLines = 1)
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
private fun AddMediaButton(rotation: Float, onClick: () -> Unit) {
    val turn by animateFloatAsState(rotation, tween(240), label = "add-upright")
    Column(
        Modifier.width(64.dp).semantics { contentDescription = "Add media"; role = Role.Button }
            .clickable(onClick = rememberTapGate(onClick)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(SliverShapes.M).background(SliverColors.Surface1)
                .border(1.dp, SliverColors.LineHairline, SliverShapes.M),
        ) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(SliverColors.Accent),
                Alignment.Center,
            ) { SliverIcon(SliverIcons.Plus, SliverColors.BgVoid, Modifier.rotate(turn), size = 14.dp) }
        }
        SliverText("ADD", SliverType.Caption, color = SliverColors.TextSecondary, maxLines = 1)
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
    val reduced = LocalReducedMotion.current
    val ring by animateColorAsState(
        if (recording) SliverColors.Accent else SliverColors.TextPrimary, motionSpec(reduced, SliverMotion.standard(SliverMotion.Quick)), label = "ring",
    )
    val inner by animateDpAsState(if (recording) 28.dp else 62.dp, motionSpec(reduced, SliverMotion.springLockIn()), label = "record-size")
    val corner by animateDpAsState(if (recording) 6.dp else 31.dp, motionSpec(reduced, SliverMotion.springLockIn()), label = "record-corner")
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
        Box(
            Modifier.size(inner).clip(RoundedCornerShape(corner.coerceAtLeast(0.dp)))
                .background(if (photo && !recording) SliverColors.TextPrimary else SliverColors.Accent),
        )
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
