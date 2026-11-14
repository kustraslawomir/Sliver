package app.sliver.feature.trim

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sliver.core.ui.SwitchRow
import app.sliver.core.log.logDebug
import app.sliver.core.ui.SlitLoader
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Color
import app.sliver.data.FilmFormat
import app.sliver.data.db.Film
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.camera.cinemaBandFraction
import app.sliver.feature.camera.cropXFromLeft
import app.sliver.feature.camera.leftFromCropX
import app.sliver.feature.camera.storyBandFraction
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.SliverFrame
import app.sliver.core.ui.SliverFrameState
import app.sliver.core.ui.StripDim
import app.sliver.data.DaySource
import app.sliver.data.LookParams
import app.sliver.data.toParams
import app.sliver.data.export.SECOND_MS
import app.sliver.data.export.ShortMode
import app.sliver.data.files.FileStore
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.core.ui.DateStamp
import app.sliver.data.export.stampText
import app.sliver.data.settings.SettingsRepository
import app.sliver.data.media.MotionPhoto
import app.sliver.feature.autoimport.CandidateStrip
import app.sliver.feature.autoimport.rememberDraftNav
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
    val files = remember { koin.get<FileStore>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val projects = remember { koin.get<ProjectRepository>() }
    val undo = remember { koin.get<SaveUndo>() }
    val draft = remember { CaptureSession.draft }
    val change = remember { CaptureSession.changeMoment }
    val draftNav = rememberDraftNav()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val today = remember { todayLocal() }
    val saved by remember { settings.settings }.collectAsStateWithLifecycle(null)

    val restoredMs = remember { savedState.get<Long>(KEY_START_MS) }
    val navigator = LocalNavigator.current

    var durationMs by remember { mutableStateOf<Long?>(null) }
    var thumbs by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var startMs by remember { mutableStateOf(restoredMs ?: change?.originalStartMs ?: 0L) }
    var committedMs by remember { mutableStateOf(restoredMs ?: change?.originalStartMs ?: 0L) }
    var custom by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf(SliverFrameState.Idle) }
    val speed = 1f
    var keepAudio by remember { mutableStateOf(change?.keepAudio ?: true) }
    var previewSound by remember { mutableStateOf(CaptureSession.previewSound) }
    LaunchedEffect(previewSound) { CaptureSession.previewSound = previewSound }
    var soundTouched by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var thumbsTotal by remember { mutableStateOf(1) }
    var shortMode by remember { mutableStateOf(ShortMode.SLOW_TO_FIT) }
    val day = remember { CaptureSession.forDate ?: today }
    val daySource = remember { CaptureSession.source }
    var format by remember { mutableStateOf(FilmFormat.CINEMA) }
    var clipAspect by remember { mutableStateOf(1f) }
    var cropY by rememberSaveable { mutableStateOf(change?.cropY ?: 0.5f) }
    var cropX by rememberSaveable { mutableStateOf(change?.cropX ?: 0.5f) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { if (!done && isTmp(path)) files.delete(path) }
    }

    LaunchedEffect(path) {
        format = projects.active.first()?.format ?: FilmFormat.CINEMA
        val d = media.durationMs(path)
        info = clipInfo(d, media.fileSizeBytes(path))
        clipAspect = media.aspect(path)
        durationMs = d
        val motion = CaptureSession.motion
        CaptureSession.motion = false
        ready = true
        // Owner: the strip starts at the beginning of the clip; no automatic jump to a "best" moment.
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
    val reframeX = format == FilmFormat.STORY && clipAspect > 1f
    val coach =saved?.coachTrimSeen == false && duration != null && !short

    fun release(to: Long) {
        startMs = to
        committedMs = to
        savedState[KEY_START_MS] = to
        dragging = false
        frameState = SliverFrameState.Locked
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Change moment re-cuts a kept recording: leaving just goes back, nothing to discard.
    val inApp = daySource == DaySource.CAMERA && change == null
    var discardOpen by remember { mutableStateOf(false) }

    fun close() {
        if (inApp) discardOpen = true else navigator.back()
    }

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = inApp && !discardOpen, onProgress = { backProgress = it }, onBack = { backProgress = 0f; close() })

    fun useSecond() {
        if (saving || !ready || durationMs == null) return
        saving = true
        scope.launch {
            val request = SaveClipRequest(
                date = day, source = path, startMs = startMs, speed = speed,
                cropX = cropX.takeIf { reframeX }, cropY = cropY.takeIf { reframe }, format = format,
                daySource = daySource, takenAt = change?.takenAt ?: Clock.System.now(), shortMode = shortMode, keepAudio = keepAudio,
                look = change?.look ?: LookParams(), motion = change?.motion ?: app.sliver.data.Motion.STILL, color = change?.color ?: 0,
                projectId = change?.projectId,
            )
            val result = if (draft != null) draftNav.keep(draft, request) else runCatching { undo.save(request) }
            if (result.isSuccess) {
                done = true
                if (draft != null) draftNav.afterResolved(day) { navigator.finishFlow(day) } else navigator.finishFlow(day)
            } else {
                result.exceptionOrNull()?.let { logDebug("TrimScreen", "save failed: $it") }
                saveFailed = true
                saving = false
            }
        }
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { (1f - 0.08f * backProgress).let { scaleX = it; scaleY = it } }
            .background(SliverColors.BgVoid),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (draft != null) app.sliver.feature.autoimport.DraftHeader(draft, draftNav, ::close) else Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton44(::close, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
                SliverText(dayLabel(day, today), SliverType.Meta, color = SliverColors.TextSecondary)
                IconButton44(
                    { previewSound = !previewSound; soundTouched = true },
                    if (previewSound) "Sound on" else "Sound off",
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
                        path, committedMs, windowMs, Modifier.fillMaxSize(), speed, previewSound, dragging,
                        scrubMs = if (dragging) startMs else null,
                    ) {
                        if (reframe) ReframeBand(cropY, { cropY = it })
                        if (reframeX) ReframeBandX(cropX, { cropX = it })
                    }
                    if (saved?.dateStamp == true) DateStamp(stampText(day))
                    if (draft != null) {
                        IconButton44(
                            { previewSound = !previewSound; soundTouched = true },
                            // DECISION: top-right, not bottom-right as on the board, so it never covers the date stamp.
                            if (previewSound) "Sound on" else "Sound off", Modifier.align(Alignment.TopEnd).padding(SliverSpace.S2),
                            overCamera = true,
                        ) { SliverIcon(if (previewSound) SliverIcons.Volume2 else SliverIcons.VolumeX, SliverColors.OnMedia) }
                    }
                }
            }
            }

            @Composable
            fun Controls() {
            if (duration != null && !short) {
                Box {
                    Filmstrip(
                        thumbs, thumbsTotal, duration, startMs, windowMs,
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
                // Owner: mute this clip in films. Separate from the preview speaker, which is only for listening here.
                SwitchRow("Sound in film", keepAudio, { keepAudio = it })
                if (draft != null) CandidateStrip(draft, draftNav)
                PrimaryButton(
                    if (saving) "Saving…" else if (!ready) "Preparing…" else if (saveFailed) "Couldn't save. Try again"
                    else if (draft != null) "Keep this clip" else "Save clip",
                    ::useSecond,
                    enabled = ready && !saving && !draftNav.busy,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    if (draft != null) {
                        GhostButton("Remove day", { if (!saving) draftNav.askSkip(draft) }, Modifier.weight(1f), destructive = true, enabled = !saving && !draftNav.busy)
                    } else {
                        GhostButton(
                            when { inApp -> "Retake"; change != null -> "Cancel"; else -> "Pick another" },
                            { if (inApp) onNavigate(Route.Camera) else navigator.back() },
                            Modifier.weight(1f),
                        )
                    }
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
        app.sliver.feature.autoimport.RemoveDayConfirm(draftNav)
        DiscardSheet(discardOpen, { discardOpen = false }, { onNavigate(Route.Camera) })
    }
}

/** Owner: thumbnails outside the frame stay readable, and a drag moves through the clip faster. */
private const val STRIP_DIM = 0.3f
private const val STRIP_DRAG_GAIN = 2.5f

@Composable
private fun Filmstrip(
    thumbs: List<ImageBitmap>,
    thumbsTotal: Int,
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
            stateDescription = "Starts at ${start / 1000}.${start / 100 % 10} s"
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
                pos -= dx / secPx * SECOND_MS * STRIP_DRAG_GAIN
                drag(pos.toLong().coerceIn(0, (durationMs - windowMs).coerceAtLeast(0)))
            }
        },
    ) {
        val density = LocalDensity.current
        val frameW = windowMs / 1000f * secPx
        val frameLeft = (constraints.maxWidth - frameW) / 2f
        val stripX = frameLeft - start / 1000f * secPx
        val tileDp = with(density) { (durationMs.toFloat() / thumbsTotal.coerceAtLeast(1) / 1000f * secPx).toDp() }
        Row(
            Modifier.wrapContentWidth(Alignment.Start, unbounded = true).fillMaxHeight()
                .offset { IntOffset(stripX.roundToInt(), 0) },
        ) {
            thumbs.forEach {
                Image(it, null, Modifier.width(tileDp).fillMaxHeight(), contentScale = ContentScale.Crop)
            }
        }
        StripDim(Modifier.width(with(density) { frameLeft.toDp() }).fillMaxHeight(), alpha = STRIP_DIM)
        StripDim(
            Modifier.fillMaxHeight().width(with(density) { (constraints.maxWidth - frameLeft - frameW).coerceAtLeast(0f).toDp() })
                .offset { IntOffset((frameLeft + frameW).roundToInt(), 0) },
            alpha = STRIP_DIM,
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
        StripDim(Modifier.fillMaxWidth().height(topDp), SliverColors.Media.copy(alpha = 0.55f))
        StripDim(Modifier.fillMaxWidth().height(h - topDp - bandH).align(Alignment.BottomStart), SliverColors.Media.copy(alpha = 0.55f))
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

/** Draggable 9:16 band over a landscape preview; everything outside is dimmed. */
@Composable
internal fun ReframeBandX(cropX: Float, onCropX: (Float) -> Unit) {
    val update by rememberUpdatedState(onCropX)
    val current by rememberUpdatedState(cropX)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val band = storyBandFraction(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val left = leftFromCropX(cropX, band)
        val w = maxWidth
        val bandW = w * band
        val leftDp = w * left
        val dim = SliverColors.Media.copy(alpha = 0.55f)
        StripDim(Modifier.fillMaxHeight().width(leftDp), dim)
        StripDim(Modifier.fillMaxHeight().width(w - leftDp - bandW).align(Alignment.BottomEnd), dim)
        Box(
            Modifier.fillMaxHeight().width(bandW).offset(x = leftDp)
                .border(1.5.dp, SliverColors.Accent)
                .semantics {
                    contentDescription = "Crop area, ${(cropX * 100).roundToInt()} percent from the left"
                    customActions = listOf(
                        CustomAccessibilityAction("Move crop left") { update((current - 0.1f).coerceIn(0f, 1f)); true },
                        CustomAccessibilityAction("Move crop right") { update((current + 0.1f).coerceIn(0f, 1f)); true },
                    )
                }
                .pointerInput(band) {
                    detectHorizontalDragGestures { change, dx ->
                        change.consume()
                        val l = (leftFromCropX(current, band) + dx / constraints.maxWidth).coerceIn(0f, 1f - band)
                        update(cropXFromLeft(l, band))
                    }
                },
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
        if (info.isNotEmpty()) SliverText(info, SliverType.Meta, color = SliverColors.TextTertiary)
    }
}
