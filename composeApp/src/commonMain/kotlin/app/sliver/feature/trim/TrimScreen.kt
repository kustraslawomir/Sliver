package app.sliver.feature.trim

import app.sliver.core.ui.SlitLoader
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Color
import app.sliver.data.FilmFormat
import app.sliver.data.db.Film
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.camera.cinemaBandFraction
import app.sliver.feature.camera.cropYFromTop
import app.sliver.feature.camera.topFromCropY
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import app.sliver.core.ui.LocalWindowSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.LocalNavigator
import app.sliver.core.navigation.Route
import androidx.lifecycle.SavedStateHandle
import app.sliver.core.ui.PredictiveBackHandler
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Badge
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SliverFrame
import app.sliver.core.ui.SliverFrameState
import app.sliver.core.ui.StripDim
import app.sliver.data.DaySource
import app.sliver.data.LookParams
import app.sliver.data.toParams
import app.sliver.feature.looks.LookSession
import app.sliver.data.export.SECOND_MS
import app.sliver.data.export.ShortMode
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.SaveSecondRequest
import app.sliver.data.settings.SettingsRepository
import app.sliver.data.media.MotionPhoto
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform
import kotlin.math.roundToInt
import kotlin.time.Clock

private const val KEY_START_MS = "trim.startMs"
private const val SECOND_DP = 72
private const val HINT_MS = 3_000L
private val StripHeight = 72.dp

private val SideColumnWidth = 400.dp

@Composable
fun TrimScreen(onNavigate: (Route) -> Unit, savedState: SavedStateHandle) {
    val path = remember { CaptureSession.path }
    if (path == null) {
        LaunchedEffect(Unit) { onNavigate(Route.Camera) }
        return
    }
    val koin = remember { KoinPlatform.getKoin() }
    val media = remember { koin.get<TrimMedia>() }
    val days = remember { koin.get<DayRepository>() }
    val files = remember { koin.get<FileStore>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val projects = remember { koin.get<ProjectRepository>() }
    val undo = remember { koin.get<SaveUndo>() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val today = remember { todayLocal() }
    val week = remember { lastSevenDays(today) }
    val filled by remember { days.observeRange(week.last(), today) }.collectAsState(emptyList())
    val saved by remember { settings.settings }.collectAsState(null)

    val resume = remember { LookSession.takeResume() }
    val restoredMs = remember { savedState.get<Long>(KEY_START_MS) }
    val navigator = LocalNavigator.current
    var look by remember { mutableStateOf(LookSession.takeApplied() ?: LookSession.params.takeIf { resume != null }) }

    var durationMs by remember { mutableStateOf<Long?>(null) }
    var thumbs by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var startMs by remember { mutableStateOf(resume?.startMs ?: restoredMs ?: 0L) }
    var committedMs by remember { mutableStateOf(resume?.startMs ?: restoredMs ?: 0L) }
    var custom by remember { mutableStateOf(resume?.custom ?: false) }
    var dragging by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf(SliverFrameState.Idle) }
    val speed = 1f
    var keepAudio by remember { mutableStateOf(resume?.audio ?: true) }
    var previewSound by remember { mutableStateOf(false) }
    var soundTouched by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var thumbsTotal by remember { mutableStateOf(1) }
    var shortMode by remember { mutableStateOf(resume?.shortMode ?: ShortMode.SLOW_TO_FIT) }
    var day by remember { mutableStateOf(resume?.day ?: CaptureSession.forDate ?: today) }
    val daySource = remember { CaptureSession.source }
    var format by remember { mutableStateOf(FilmFormat.CINEMA) }
    var clipAspect by remember { mutableStateOf(1f) }
    var cropY by remember { mutableStateOf(0.5f) }
    var sheet by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { if (!done && isTmp(path)) files.delete(path) }
    }

    LaunchedEffect(path) {
        if (look == null) look = settings.settings.first().defaultLook.toParams()
        format = projects.active.first()?.format ?: FilmFormat.CINEMA
        val d = media.durationMs(path)
        info = clipInfo(d, media.fileSizeBytes(path))
        clipAspect = media.aspect(path)
        durationMs = d
        val motion = CaptureSession.motion
        CaptureSession.motion = false
        ready = true
        if (resume == null && restoredMs == null) {
            if (motion) {
                MotionPhoto.windowStartMs(CaptureSession.motionPresentationUs, d).let { startMs = it; committedMs = it }
            } else {
                // Scoring can take a while on slow decoders: the clip is usable at 0 s meanwhile, and the pick applies only if the user has not moved yet.
                scope.launch {
                    val pick = media.autoPickMs(path, d)
                    if (!custom && startMs == committedMs) {
                        startMs = pick
                        committedMs = pick
                    }
                }
            }
        }
        if (!isShortClip(d)) {
            thumbsTotal = thumbCount(d)
            media.thumbnails(path, d, thumbsTotal) { thumbs = thumbs + it }
        }
    }
    LaunchedEffect(soundTouched) {
        if (!soundTouched) {
            delay(HINT_MS)
            soundTouched = true
        }
    }
    LaunchedEffect(frameState) {
        if (frameState == SliverFrameState.Locked) {
            delay(400)
            frameState = SliverFrameState.Idle
        }
    }

    val duration = durationMs
    val short = duration != null && isShortClip(duration)
    val windowMs = if (short) duration ?: 0 else (SECOND_MS * speed).toLong()
    val reframe = format == FilmFormat.CINEMA && clipAspect < 1f
    val coach =saved?.coachTrimSeen == false && duration != null && !short

    fun release(to: Long) {
        startMs = to
        committedMs = to
        savedState[KEY_START_MS] = to
        dragging = false
        frameState = SliverFrameState.Locked
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val inApp = daySource == DaySource.CAMERA
    var discardOpen by remember { mutableStateOf(false) }

    fun close() {
        if (inApp) discardOpen = true else navigator.back()
    }

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = inApp && !discardOpen, onProgress = { backProgress = it }, onBack = { backProgress = 0f; close() })

    fun openLooks() {
        val duration = durationMs ?: return
        LookSession.isPhoto = false
        LookSession.startMs = committedMs
        LookSession.windowMs = if (isShortClip(duration)) duration else (SECOND_MS * speed).toLong()
        LookSession.speed = speed
        LookSession.params = look ?: LookParams()
        LookSession.resume = LookSession.Resume(startMs, custom, speed, keepAudio, shortMode, day)
        done = true
        onNavigate(Route.Looks)
    }

    fun useSecond() {
        if (saving || !ready || durationMs == null) return
        saving = true
        scope.launch {
            val request = SaveSecondRequest(
                date = day, source = path, startMs = startMs, speed = speed,
                cropY = cropY.takeIf { reframe }, format = format,
                daySource = daySource, takenAt = Clock.System.now(), shortMode = shortMode, keepAudio = keepAudio,
                look = look ?: settings.settings.first().defaultLook.toParams(),
            )
            val result = runCatching { undo.save(request, dayLabel(day, today)) }
            if (result.isSuccess) {
                done = true
                navigator.finishFlow(day)
            } else {
                result.exceptionOrNull()?.let { println("SliverSave failed: $it") }
                undo.toast.show("Couldn't save the clip. Try again.")
                saving = false
            }
        }
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { (1f - 0.08f * backProgress).let { scaleX = it; scaleY = it } }
            .background(SliverColors.BgVoid),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton44(::close, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
                Chip(dayLabel(day, today).uppercase(), selected = false, onClick = { sheet = true }, trailing = SliverIcons.ChevronDown)
                IconButton44(
                    { previewSound = !previewSound; keepAudio = previewSound; soundTouched = true },
                    if (previewSound) "Mute" else "Unmute",
                ) {
                    SliverIcon(
                        if (previewSound) SliverIcons.Volume2 else SliverIcons.VolumeX,
                        if (previewSound) SliverColors.Accent else SliverColors.TextTertiary,
                    )
                }
            }

            val sideBySide = LocalWindowSize.current.sideBySide
            @Composable
            fun Preview(m: Modifier) {
            Box(
                m.padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2)
                    .clip(SliverShapes.S).background(SliverColors.Surface1),
            ) {
                if (!ready) {
                    PrepareSkeleton(
                        prepareProgress(duration != null, ready, thumbs.size, thumbsTotal), info, Modifier.align(Alignment.Center),
                    )
                } else if (duration != null) {
                    ClipPlayer(
                        path, committedMs, windowMs, Modifier.fillMaxSize(), speed, previewSound, dragging, look ?: LookParams(),
                    ) { if (reframe) ReframeBand(cropY, { cropY = it }) }
                    if (!soundTouched) {
                        SliverText(
                            "Clip keeps its sound", SliverType.BodySmall,
                            Modifier.align(Alignment.BottomCenter).padding(SliverSpace.S3)
                                .clip(SliverShapes.Pill).background(SliverColors.Surface2)
                                .padding(horizontal = SliverSpace.S4, vertical = SliverSpace.S2),
                            SliverColors.TextPrimary,
                        )
                    }
                    Badge(
                        if (custom) "Custom" else "Auto-picked",
                        Modifier.align(Alignment.TopStart).padding(SliverSpace.S3),
                    )
                }
            }
            }

            @Composable
            fun Controls() {
            if (duration != null && showMinimap(duration)) {
                Minimap(startMs, windowMs, duration) { release(minimapJump(it, duration, speed)); custom = true }
            }

            if (duration != null && !short) {
                Box {
                    Filmstrip(
                        thumbs, duration, startMs, windowMs,
                        frameState = frameState,
                        onDragStart = {
                            dragging = true
                            frameState = SliverFrameState.Dragging
                            if (coach) scope.launch { settings.update { it.copy(coachTrimSeen = true) } }
                        },
                        onDrag = {
                            val next = clampStart(it, duration, speed)
                            if (tickIndex(next) != tickIndex(startMs)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            startMs = next
                            custom = true
                        },
                        onDragEnd = { release(startMs) },
                    )
                    if (coach) {
                        SliverText(
                            "Drag to choose your clip.", SliverType.BodySmall,
                            Modifier.align(Alignment.TopCenter).offset(y = (-28).dp)
                                .clip(SliverShapes.Pill).background(SliverColors.Surface2)
                                .padding(horizontal = SliverSpace.S4, vertical = SliverSpace.S2),
                            SliverColors.TextPrimary,
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3),
                verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
            ) {
                if (short) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        listOf(ShortMode.SLOW_TO_FIT to "Slow to fit", ShortMode.LOOP to "Loop", ShortMode.HOLD_LAST to "Hold last frame")
                            .forEach { (mode, label) -> Chip(label, shortMode == mode, { shortMode = mode }) }
                    }
                }
                PrimaryButton(
                    if (saving) "Saving…" else if (!ready) "Preparing…" else "Save clip", ::useSecond,
                    enabled = ready && !saving,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    GhostButton(
                        if (inApp) "Retake" else "Pick another",
                        { if (inApp) onNavigate(Route.Camera) else navigator.back() },
                        Modifier.weight(1f),
                    )
                    GhostButton("Filters", ::openLooks, Modifier.weight(1f))
                }
            }
            }

            if (sideBySide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Preview(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(SideColumnWidth).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) { Controls() }
                }
            } else {
                Preview(Modifier.weight(1f).fillMaxWidth())
                Controls()
            }
        }
        DiscardSheet(discardOpen, { discardOpen = false }, { onNavigate(Route.Camera) })
        SliverBottomSheet(sheet, { sheet = false }) {
            Column {
                SliverText("Save to which day", SliverType.Headline, color = SliverColors.TextPrimary)
                week.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).clickable { day = d; sheet = false },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SliverText(
                            dayLabel(d, today), SliverType.Body,
                            color = if (d == day) SliverColors.Accent else SliverColors.TextPrimary,
                        )
                        if (filled.any { it.date == d }) SliverText("HAS A CLIP", SliverType.MonoS, color = SliverColors.TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Filmstrip(
    thumbs: List<ImageBitmap>,
    durationMs: Long,
    startMs: Long,
    windowMs: Long,
    frameState: SliverFrameState,
    onDragStart: () -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
) {
    val secPx = with(LocalDensity.current) { SECOND_DP.dp.toPx() }
    val start by rememberUpdatedState(startMs)
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(StripHeight).background(SliverColors.Surface1).semantics {
            contentDescription = "Trim position"
            stateDescription = "Starts at ${start / 1000}.${start / 100 % 10} seconds"
            val maxStart = (durationMs - windowMs).coerceAtLeast(0).toFloat()
            progressBarRangeInfo = ProgressBarRangeInfo(start.toFloat(), 0f..maxStart.coerceAtLeast(1f), (maxStart / 100).toInt().coerceAtLeast(1) - 1)
            setProgress { v ->
                val target = ((v / 100f).roundToInt() * 100L).coerceIn(0, maxStart.toLong())
                dragStart(); drag(target); dragEnd()
                true
            }
        }.pointerInput(secPx) {
            var pos = 0f
            detectHorizontalDragGestures(
                onDragStart = { pos = start.toFloat(); dragStart() },
                onDragEnd = { dragEnd() },
                onDragCancel = { dragEnd() },
            ) { change, dx ->
                change.consume()
                pos -= dx / secPx * SECOND_MS
                drag(pos.toLong().coerceIn(0, (durationMs - windowMs).coerceAtLeast(0)))
            }
        },
    ) {
        val density = LocalDensity.current
        val frameW = windowMs / 1000f * secPx
        val frameLeft = (constraints.maxWidth - frameW) / 2f
        val stripX = frameLeft - start / 1000f * secPx
        val tileDp = with(density) { (durationMs.toFloat() / thumbs.size.coerceAtLeast(1) / 1000f * secPx).toDp() }
        Row(
            Modifier.wrapContentWidth(Alignment.Start, unbounded = true).fillMaxHeight()
                .offset { IntOffset(stripX.roundToInt(), 0) },
        ) {
            thumbs.forEach {
                Image(it, null, Modifier.width(tileDp).fillMaxHeight(), contentScale = ContentScale.Crop)
            }
        }
        StripDim(Modifier.width(with(density) { frameLeft.toDp() }).fillMaxHeight())
        StripDim(
            Modifier.fillMaxHeight().width(with(density) { (constraints.maxWidth - frameLeft - frameW).coerceAtLeast(0f).toDp() })
                .offset { IntOffset((frameLeft + frameW).roundToInt(), 0) },
        )
        SliverFrame(
            frameState,
            Modifier.width(with(density) { frameW.toDp() }).fillMaxHeight()
                .offset { IntOffset(frameLeft.roundToInt(), 0) },
        )
    }
}

/** Draggable 16:9 band over a portrait preview; everything outside is dimmed. */
@Composable
private fun ReframeBand(cropY: Float, onCropY: (Float) -> Unit) {
    val update by rememberUpdatedState(onCropY)
    val current by rememberUpdatedState(cropY)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val band = cinemaBandFraction(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val top = topFromCropY(cropY, band)
        val h = maxHeight
        val bandH = h * band
        val topDp = h * top
        StripDim(Modifier.fillMaxWidth().height(topDp), Color.Black.copy(alpha = 0.55f))
        StripDim(Modifier.fillMaxWidth().height(h - topDp - bandH).align(Alignment.BottomStart), Color.Black.copy(alpha = 0.55f))
        Box(
            Modifier.fillMaxWidth().height(bandH).offset(y = topDp)
                .border(1.5.dp, SliverColors.Accent)
                .pointerInput(band) {
                    detectVerticalDragGestures { change, dy ->
                        change.consume()
                        val t = (topFromCropY(current, band) + dy / constraints.maxHeight).coerceIn(0f, 1f - band)
                        update(cropYFromTop(t, band))
                    }
                },
        )
    }
}

@Composable
private fun Minimap(startMs: Long, windowMs: Long, durationMs: Long, onJump: (Float) -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2).height(24.dp)
            .clip(SliverShapes.S).background(SliverColors.Surface2)
            .pointerInput(Unit) { detectTapGestures { onJump(it.x / size.width) } },
    ) {
        val w = maxWidth
        val left = w * (startMs.toFloat() / durationMs)
        val width = (w * (windowMs.toFloat() / durationMs)).coerceAtLeast(3.dp)
        Box(
            Modifier.offset(left, 0.dp).width(width).fillMaxHeight()
                .border(1.5.dp, SliverColors.Accent, SliverShapes.S),
        )
    }
}

internal fun dayLabel(date: LocalDate, today: LocalDate): String {
    return when ((today.toEpochDays() - date.toEpochDays()).toLong()) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> "${date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}"
    }
}

@Composable
private fun PrepareSkeleton(progress: Float, info: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SlitLoader()
        SliverText("Preparing video… ${(progress * 100).roundToInt()}%", SliverType.Body, color = SliverColors.TextPrimary)
        if (info.isNotEmpty()) SliverText(info, SliverType.MonoS, color = SliverColors.TextTertiary)
    }
}
