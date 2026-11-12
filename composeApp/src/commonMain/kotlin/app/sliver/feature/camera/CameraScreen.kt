package app.sliver.feature.camera

import app.sliver.feature.gallery.rememberAddMedia
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.motionSpec
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.semantics.selected
import app.sliver.core.ui.SwitchRow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.defaultMinSize
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
import app.sliver.core.ui.rememberTapGate
import app.sliver.feature.archive.ArchiveSession
import app.sliver.data.repo.DayRepository
import app.sliver.core.ui.Segmented
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.files.FileStore
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.CameraSettings
import app.sliver.data.settings.Lens
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import androidx.compose.animation.core.animate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
fun CameraScreen(onNavigate: (Route) -> Unit, onClose: () -> Unit) {
    val addMedia = rememberAddMedia(onNavigate)
    PermissionGate(
        PermissionKind.CameraMic,
        onDeniedAlternative = { addMedia(null) },
    ) {
        CameraContent(onNavigate, addMedia, onClose)
    }
}

@Composable
private fun CameraContent(onNavigate: (Route) -> Unit, addMedia: (kotlinx.datetime.LocalDate?) -> Unit, onClose: () -> Unit) {
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
    var menuOpen by remember { mutableStateOf(false) }
    var flipTurns by remember { mutableIntStateOf(0) }
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)
    val format = project?.format ?: saved?.newFilmFormat ?: FilmFormat.CINEMA
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
    var zoom by rememberSaveable(cam.lens) { mutableFloatStateOf(1f) }
    var zoomRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(zoom) { engine.setZoom(zoom) }
    fun zoomTo(target: Float) {
        zoomJob?.cancel()
        zoomJob = scope.launch { animate(zoom, target, animationSpec = tween(220)) { v, _ -> zoom = v } }
    }
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

    var failed by remember { mutableStateOf(false) }

    fun finish(result: Result<String>, route: Route) {
        recording = false
        holding = false
        result.onSuccess {
            CaptureSession.path = it
            CaptureSession.source = app.sliver.data.DaySource.CAMERA
            CaptureSession.forDate = null
            onNavigate(route)
        }.onFailure {
            failed = true
        }
    }

    var storageFull by remember { mutableStateOf(false) }
    fun storageOk(): Boolean = !isStorageLow(files.freeSpaceBytes()).also { storageFull = it }

    fun startRecording() {
        if (!storageOk()) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        elapsedMs = 0
        recording = true
        failed = false
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

    fun onTap() {
        when {
            recording -> stopRecording()
            cam.mode == CameraMode.PHOTO -> capturePhoto()
            else -> startRecording()
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
            CameraPreview(engine, cam.mode, cam.lens, cam.flash, Modifier.fillMaxSize()) {
                ready = true
                zoomRange = engine.zoomRange()
            }
            if (bandActive) CinemaBandDim()
            if (cam.grid) GridOverlay()
            FocusOverlay(!recording && ready, cam.lens to cam.mode, engine::exposureRange, engine::focus, engine::setExposureIndex, { factor ->
                zoomJob?.cancel()
                zoomRange?.let { zoom = (zoom * factor).coerceIn(it) }
            })
            zoomRange?.let { r ->
                ZoomStops(zoomPresets(r.start, r.endInclusive), zoom, iconRot, Modifier.align(Alignment.BottomCenter).padding(bottom = SliverSpace.S4)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    zoomTo(it)
                }
            }
        }

        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(150.dp).background(
                Brush.verticalGradient(
                    0f to SliverColors.Media.copy(alpha = 0.7f), 0.55f to SliverColors.Media.copy(alpha = 0.35f), 1f to Color.Transparent,
                ),
            ),
        )

        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().widthIn(max = MaxContentWidth)
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3)
                .then(if (side) Modifier.padding(end = SideControlsWidth) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (recording) {
                    val long = clipLongEnough(elapsedMs)
                    LaunchedEffect(long) { if (long) haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
                    TimerPill(formatTimer(elapsedMs), long, Modifier.align(Alignment.CenterVertically))
                } else {
                    CameraRoundButton(onClose, "Close camera") { SliverIcon(SliverIcons.X, SliverColors.OnMedia) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { GoldenHourLine(saved?.showGoldenHour == true) }
                    LightToggle(cam.flash, iconRot) { update { it.copy(flash = !it.flash) } }
                    Spacer(Modifier.width(SliverSpace.S2))
                    CameraRoundButton({ menuOpen = true }, "More camera options") { SliverIcon(SliverIcons.MoreVertical, SliverColors.OnMedia) }
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
            if (failed && !recording) {
                SliverText(
                    "That recording didn't work. Try again.", SliverType.BodySmall,
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

        Column(
            (if (side) Modifier.align(Alignment.CenterEnd).width(SideControlsWidth).statusBarsPadding()
            else Modifier.align(Alignment.BottomCenter).widthIn(max = MaxContentWidth).fillMaxWidth())
                .navigationBarsPadding().padding(bottom = SliverSpace.S4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3, if (side) Alignment.CenterVertically else Alignment.Top),
        ) {
            Box(Modifier.alpha(if (recording) 0f else 1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                ModeWord("Photo", cam.mode == CameraMode.PHOTO) { if (!recording) update { s -> s.copy(mode = CameraMode.PHOTO) } }
                ModeWord("Video", cam.mode == CameraMode.VIDEO) { if (!recording) update { s -> s.copy(mode = CameraMode.VIDEO) } }
            }
            if (recording) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (recording) Box(Modifier.size(64.dp)) else CameraAction(SliverIcons.ImagePlus, "Add", "Add media", iconRot) { addMedia(null) }
                RecordButton(
                    state = when {
                        recording -> RecordState.Recording
                        cam.mode == CameraMode.PHOTO -> RecordState.Photo
                        else -> RecordState.VideoIdle
                    },
                    holding = holding,
                    enabled = ready,
                    onTap = ::onTap,
                    onHoldStart = {
                        if (cam.mode == CameraMode.VIDEO && !recording) {
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
                    CameraAction(SliverIcons.SwitchCamera, "Flip", "Switch camera", turn) {
                        flipTurns++
                        update { it.copy(lens = if (it.lens == Lens.BACK) Lens.FRONT else Lens.BACK) }
                    }
                }
            }
        }
        SliverBottomSheet(menuOpen, { menuOpen = false }) {
            Column {
                SwitchRow("Grid", cam.grid, { on -> update { it.copy(grid = on) } })
            }
        }

        if (storageFull) StorageFullOverlay({ storageFull = false })
    }
}

@Composable
private fun CinemaBandDim() {
    Canvas(Modifier.fillMaxSize()) {
        val band = cinemaBandFraction(size.width, size.height) * size.height
        val edge = (size.height - band) / 2f
        val dim = SliverColors.Media.copy(alpha = 0.55f)
        drawRect(dim, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, edge))
        drawRect(dim, Offset(0f, size.height - edge), androidx.compose.ui.geometry.Size(size.width, edge))
    }
}

@Composable
private fun ModeWord(text: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.defaultMinSize(minHeight = 44.dp)
            .semantics { selected = active; role = Role.Tab }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SliverText(
            text, SliverType.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp).overMedia(),
            color = SliverColors.OnMedia.copy(alpha = if (active) 1f else 0.8f), maxLines = 1,
        )
        Box(Modifier.padding(top = 2.dp).size(4.dp).background(if (active) SliverColors.OnMedia else Color.Transparent, CircleShape))
    }
}

@Composable
private fun CameraAction(icon: ImageVector, label: String, description: String, rotation: Float, onClick: () -> Unit) {
    val turn by animateFloatAsState(rotation, tween(240), label = "action-turn")
    Column(
        Modifier.width(64.dp).semantics { contentDescription = description; role = Role.Button }
            .clickable(onClick = rememberTapGate(onClick)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(SliverColors.OnMedia.copy(alpha = 0.1f)), Alignment.Center) {
            SliverIcon(icon, SliverColors.OnMedia, Modifier.rotate(turn), size = 22.dp)
        }
        SliverText(label, SliverType.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp).overMedia(), color = SliverColors.OnMedia, maxLines = 1)
    }
}

// DECISION: no design for zoom yet; stops follow One UI (small dark circles, the active one larger with the live ratio and ×).
@Composable
private fun ZoomStops(presets: List<Float>, zoom: Float, rotation: Float, modifier: Modifier, onSelect: (Float) -> Unit) {
    if (presets.size < 2) return
    val active = activeZoomPreset(presets, zoom)
    val turn by animateFloatAsState(rotation, tween(240), label = "zoom-upright")
    Row(
        modifier.clip(SliverShapes.Pill).background(SliverColors.Media.copy(alpha = 0.35f)).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEachIndexed { i, preset ->
            val on = i == active
            val label = formatZoom(if (on) zoom else preset, unit = on)
            Box(
                Modifier.size(44.dp)
                    .semantics { contentDescription = "Zoom ${formatZoom(preset, true)}"; selected = on; role = Role.Button }
                    .clickable { onSelect(preset) },
                Alignment.Center,
            ) {
                Box(
                    Modifier.size(if (on) 38.dp else 30.dp).clip(CircleShape).background(SliverColors.Media.copy(alpha = if (on) 0.7f else 0.5f)).rotate(turn),
                    Alignment.Center,
                ) {
                    SliverText(
                        label, SliverType.Meta.copy(fontSize = if (on) 12.sp else 11.sp, fontWeight = FontWeight.SemiBold).overMedia(),
                        color = SliverColors.OnMedia.copy(alpha = if (on) 1f else 0.85f), maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val c = SliverColors.OnMedia.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = size.width * i / 3
            val y = size.height * i / 3
            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
    }
}

@Composable
private fun CameraRoundButton(onClick: () -> Unit, label: String, icon: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(SliverColors.Media.copy(alpha = 0.5f))
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = rememberTapGate(onClick)),
        Alignment.Center,
    ) { icon() }
}

@Composable
private fun TimerPill(time: String, clipOk: Boolean, modifier: Modifier = Modifier) {
    val color = if (clipOk) SliverColors.Record else SliverColors.TextSecondary
    val reduced = LocalReducedMotion.current
    val dotAlpha = if (reduced) 1f else {
        val t by rememberInfiniteTransition(label = "dot").animateFloat(
            1f, 0.78f, infiniteRepeatable(tween(800, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), RepeatMode.Reverse), label = "dot",
        )
        t
    }
    Row(
        modifier.height(34.dp).clip(SliverShapes.Pill).background(SliverColors.BgVoid.copy(alpha = 0.7f))
            .border(1.dp, if (clipOk) SliverColors.Record.copy(alpha = 0.4f) else SliverColors.LineStrong, SliverShapes.Pill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
    ) {
        Box(Modifier.size(8.dp).alpha(dotAlpha).background(SliverColors.Record, CircleShape))
        SliverText(time, SliverType.Num, color = color, maxLines = 1)
        if (clipOk) SliverText("\u00B7 Clip OK", SliverType.Meta, color = color, maxLines = 1)
    }
}

@Composable
private fun LightToggle(on: Boolean, rotation: Float, onClick: () -> Unit) {
    val turn by animateFloatAsState(rotation, tween(240), label = "light-upright")
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(SliverColors.Media.copy(alpha = 0.5f))
            .semantics { contentDescription = if (on) "Light on" else "Light off"; role = Role.Switch; selected = on }
            .clickable(role = Role.Switch, onClick = rememberTapGate(onClick)),
        Alignment.Center,
    ) {
        SliverIcon(if (on) SliverIcons.Zap else SliverIcons.ZapOff, if (on) SliverColors.OnMedia else SliverColors.OnMedia.copy(alpha = 0.7f), Modifier.rotate(turn))
    }
}
