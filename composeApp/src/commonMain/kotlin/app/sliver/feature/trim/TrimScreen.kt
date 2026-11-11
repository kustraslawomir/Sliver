package app.sliver.feature.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
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
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SliverFrame
import app.sliver.core.ui.SliverFrameState
import app.sliver.core.ui.StripDim
import app.sliver.data.DaySource
import app.sliver.data.export.SECOND_MS
import app.sliver.data.export.ShortMode
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.SaveSecondRequest
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform
import kotlin.math.roundToInt
import kotlin.time.Clock

private const val SECOND_DP = 72
private val StripHeight = 72.dp

@Composable
fun TrimScreen(onNavigate: (Route) -> Unit) {
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
    val undo = remember { koin.get<SaveUndo>() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val today = remember { todayLocal() }
    val week = remember { lastSevenDays(today) }
    val filled by remember { days.observeRange(week.last(), today) }.collectAsState(emptyList())
    val saved by remember { settings.settings }.collectAsState(null)

    var durationMs by remember { mutableStateOf<Long?>(null) }
    var thumbs by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var startMs by remember { mutableStateOf(0L) }
    var committedMs by remember { mutableStateOf(0L) }
    var custom by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf(SliverFrameState.Idle) }
    var speed by remember { mutableStateOf(1f) }
    var audio by remember { mutableStateOf(true) }
    var shortMode by remember { mutableStateOf(ShortMode.SLOW_TO_FIT) }
    var day by remember { mutableStateOf(CaptureSession.forDate ?: today) }
    val daySource = remember { CaptureSession.source }
    var sheet by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { if (!done && isTmp(path)) files.delete(path) }
    }

    LaunchedEffect(path) {
        val d = media.durationMs(path)
        val pick = media.autoPickMs(path, d)
        startMs = pick
        committedMs = pick
        durationMs = d
        if (!isShortClip(d)) thumbs = media.thumbnails(path, d, thumbCount(d))
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
    val coach = saved?.coachTrimSeen == false && duration != null && !short

    fun release(to: Long) {
        startMs = to
        committedMs = to
        dragging = false
        frameState = SliverFrameState.Locked
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun close() {
        onNavigate(Route.Camera)
    }

    fun useSecond() {
        if (saving || duration == null) return
        saving = true
        scope.launch {
            val format = settings.settings.first().newFilmFormat
            val request = SaveSecondRequest(
                date = day, source = path, startMs = startMs, speed = speed, cropY = null, format = format,
                daySource = daySource, takenAt = Clock.System.now(), shortMode = shortMode, keepAudio = audio,
            )
            val ok = runCatching { undo.save(request, dayLabel(day, today)) }.isSuccess
            saving = false
            if (ok) {
                done = true
                close()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton44(::close, "Close") { SliverText("X", SliverType.MonoS, color = SliverColors.TextPrimary) }
                Chip("FOR ${dayLabel(day, today).uppercase()} v", selected = false, onClick = { sheet = true })
                IconButton44({ audio = !audio }, if (audio) "Mute" else "Unmute") {
                    SliverText(if (audio) "SND" else "OFF", SliverType.MonoS, color = if (audio) SliverColors.Accent else SliverColors.TextTertiary)
                }
            }

            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2)
                    .clip(SliverShapes.S).background(SliverColors.Surface1),
            ) {
                if (duration != null) {
                    TrimPlayer(path, committedMs, windowMs, speed, audio, dragging, Modifier.fillMaxSize())
                    Badge(
                        if (custom) "Custom" else "✦ Auto-picked",
                        Modifier.align(Alignment.TopStart).padding(SliverSpace.S3),
                    )
                }
            }

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
                            "Drag to choose your second.", SliverType.BodySmall,
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
                } else if (duration != null) {
                    Segmented(listOf("1×", "0.5×"), if (speed == 1f) 0 else 1, {
                        speed = if (it == 0) 1f else 0.5f
                        startMs = clampStart(startMs, duration, speed)
                        committedMs = startMs
                    })
                }
                PrimaryButton(
                    if (saving) "Saving..." else "Use this second", ::useSecond,
                    enabled = duration != null && !saving,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    GhostButton("Retake", ::close, Modifier.weight(1f))
                    GhostButton("Looks", { onNavigate(Route.Looks) }, Modifier.weight(1f))
                }
            }
        }
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
                        if (filled.any { it.date == d }) SliverText("HAS A SECOND", SliverType.MonoS, color = SliverColors.TextTertiary)
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
        Modifier.fillMaxWidth().height(StripHeight).background(SliverColors.Surface1).pointerInput(secPx) {
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
