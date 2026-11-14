package app.sliver.feature.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** True when a second finger lands (pinch), false when the gesture ends or something else takes it. */
private suspend fun AwaitPointerEventScope.awaitUpOrSecondFinger(): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return true
        if (event.changes.none { it.pressed } || event.changes.any { it.isConsumed }) return false
    }
}

private suspend fun AwaitPointerEventScope.pinch(onZoomBy: (Float) -> Unit) {
    do {
        val event = awaitPointerEvent()
        val factor = event.calculateZoom()
        if (factor != 1f) onZoomBy(factor)
        event.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
    } while (event.changes.any { it.pressed })
}

private data class FocusState(val x: Float, val y: Float, val tap: Int, val locked: Boolean = false, val ok: Boolean? = null)

/**
 * Tap-to-focus layer. It sits over the preview and under the camera controls, so its coordinates are PreviewView
 * coordinates and taps on controls never reach it.
 * Tap: thin AF+AE ring like One UI (84 -> 70 dp, 220 ms, dims to 35% after 1.2 s or at once if focus failed).
 * Hold 600 ms: AE/AF lock. A vertical exposure slider appears beside the ring.
 */
@Composable
fun FocusOverlay(
    enabled: Boolean,
    resetKey: Any,
    exposureRange: () -> ExposureRange?,
    onFocus: (x: Float, y: Float, lock: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    onExposure: (Int) -> Unit,
    onZoomBy: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoomBy by rememberUpdatedState(onZoomBy)
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var focus by remember { mutableStateOf<FocusState?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var range by remember { mutableStateOf<ExposureRange?>(null) }
    var evIndex by remember { mutableIntStateOf(0) }
    var interaction by remember { mutableIntStateOf(0) }
    var sliderVisible by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, resetKey) {
        focus = null
        evIndex = 0
        sliderVisible = false
    }
    LaunchedEffect(interaction, focus?.locked) {
        if (interaction == 0 || focus?.locked == true) return@LaunchedEffect
        sliderVisible = true
        delay(EXPOSURE_HIDE_AFTER_MS)
        sliderVisible = false
    }

    Box(
        modifier.fillMaxSize().onSizeChanged { size = it }
            .semantics { contentDescription = "Viewfinder. Tap to focus, hold to lock focus and exposure" }
            .pointerInput(enabled) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!enabled) {
                        if (awaitUpOrSecondFinger()) pinch(zoomBy)
                        return@awaitEachGesture
                    }
                    val (cx, cy) = focusRingCenter(
                        down.position.x,
                        down.position.y,
                        size.width.toFloat(),
                        size.height.toFloat(),
                        with(density) { FOCUS_RING_START_DP.dp.toPx() },
                    )
                    val tapId = (focus?.tap ?: 0) + 1
                    focus = FocusState(cx, cy, tapId)
                    range = exposureRange()
                    interaction++
                    onFocus(down.position.x, down.position.y, false) { ok ->
                        if (focus?.tap == tapId) focus = focus?.copy(ok = ok)
                    }
                    val secondFinger = withTimeoutOrNull(FOCUS_LOCK_HOLD_MS) { awaitUpOrSecondFinger() }
                    if (secondFinger == true) {
                        focus = null
                        pinch(zoomBy)
                    } else if (secondFinger == null) {
                        focus = FocusState(cx, cy, tapId, locked = true)
                        onFocus(down.position.x, down.position.y, true) { ok ->
                            if (focus?.tap == tapId) focus = focus?.copy(ok = ok)
                        }
                        waitForUpOrCancellation()
                    }
                }
            },
    ) {
        val f = focus ?: return@Box
        FocusRing(f, reduced, Offset(f.x, f.y), if (enabled && (sliderVisible || f.locked)) 1f else 0f)
        val r = range
        if (r != null && enabled) {
            ExposureSlider(
                visible = sliderVisible || f.locked, locked = f.locked, range = r, index = evIndex,
                centerX = f.x, centerY = f.y, width = size.width.toFloat(), height = size.height.toFloat(),
                onIndex = {
                    if (it != evIndex) {
                        evIndex = it
                        onExposure(it)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onTouch = { interaction++ },
            )
        }
    }
}

@Composable
private fun FocusRing(f: FocusState, reduced: Boolean, center: Offset, visibility: Float) {
    val ringSize =
        remember(f.tap) { Animatable(if (reduced) FOCUS_RING_END_DP.toFloat() else FOCUS_RING_START_DP.toFloat()) }
    val opacity = remember(f.tap) { Animatable(1f) }
    LaunchedEffect(f.tap) {
        if (!reduced) ringSize.animateTo(FOCUS_RING_END_DP.toFloat(), tween(220, easing = SliverMotion.EaseStandard))
    }
    LaunchedEffect(f.tap, f.locked, f.ok) {
        when {
            f.locked -> opacity.snapTo(1f)
            f.ok == false -> opacity.animateTo(0.35f, tween(200))
            else -> {
                delay(FOCUS_DIM_AFTER_MS)
                opacity.animateTo(0.35f, tween(400))
            }
        }
    }
    val color = if (f.locked) SliverColors.Accent else SliverColors.OnMedia
    val d = ringSize.value
    val fade by animateFloatAsState(visibility, tween(300), label = "ring-fade")
    Box(Modifier.fillMaxSize().alpha(opacity.value * fade)) {
        Box(
            Modifier.offset { IntOffset((center.x - d.dp.toPx() / 2).roundToInt(), (center.y - d.dp.toPx() / 2).roundToInt()) }
                .size(d.dp)
                .border(1.5.dp, color, CircleShape)
        )
        if (f.locked) {
            Box(
                Modifier.layout { m, c ->
                    val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                    layout(p.width, p.height) {
                        p.place((center.x - p.width / 2f).roundToInt(), (center.y - FOCUS_LOCK_PILL_OFFSET_DP.dp.toPx()).roundToInt())
                    }
                },
            ) {
                SliverText(
                    "AE/AF lock",
                    SliverType.Body.copy(
                        fontSize = TextUnit(12f, TextUnitType.Sp),
                        lineHeight = TextUnit(16f, TextUnitType.Sp),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    Modifier.background(SliverColors.Accent, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    SliverColors.OnAccent,
                    maxLines = 1,
                )
            }
        }
    }
}

private val SliderWidth = 32.dp
private val SliderGap = 48.dp

/** Neutral white vertical slider beside the focus ring; accent only while AE/AF is locked. */
@Composable
private fun ExposureSlider(
    visible: Boolean,
    locked: Boolean,
    range: ExposureRange,
    index: Int,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    onIndex: (Int) -> Unit,
    onTouch: () -> Unit,
) {
    val density = LocalDensity.current
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "ev-fade")
    if (alpha == 0f) return
    val trackPx = with(density) { EXPOSURE_TRACK_DP.dp.toPx() }
    val onRight = exposureOnRight(centerX, width)
    val gapPx = with(density) { SliderGap.toPx() }
    val widthPx = with(density) { SliderWidth.toPx() }
    val left = if (onRight) centerX + gapPx - widthPx / 2 else centerX - gapPx - widthPx / 2
    val top = (centerY - trackPx / 2).coerceIn(0f, (height - trackPx).coerceAtLeast(0f))
    val fraction = exposureFraction(index, range.min, range.max)
    val tint = if (locked) SliverColors.Accent else SliverColors.OnMedia
    Box(
        Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .width(SliderWidth).height(EXPOSURE_TRACK_DP.dp).alpha(alpha)
            .semantics { contentDescription = "Brightness ${formatEv(index, range.step)}" }
            .pointerInput(range) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onTouch()
                    onIndex(exposureIndexAt(down.position.y / size.height, range.min, range.max))
                    verticalDrag(down.id) { change ->
                        change.consume()
                        onTouch()
                        onIndex(exposureIndexAt(change.position.y / size.height, range.min, range.max))
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.align(Alignment.Center).width(1.5.dp).fillMaxSize().background(SliverColors.OnMedia.copy(alpha = 0.85f), CircleShape))
        val thumb = 32.dp
        Box(
            Modifier.offset { IntOffset(0, ((trackPx - with(density) { thumb.toPx() }) * fraction).roundToInt()) }
                .size(thumb).background(SliverColors.Media.copy(alpha = 0.55f), CircleShape).border(1.dp, tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { SliverIcon(SliverIcons.Sun, tint, size = 18.dp) }
        SliverText(
            formatEv(index, range.step),
            SliverType.Meta.overMedia(),
            Modifier.layout { m, c ->
                val p = m.measure(c.copy(minWidth = 0, minHeight = 0, maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
                val gap = 22.dp.roundToPx()
                val x = if (onRight) c.maxWidth / 2 + gap else c.maxWidth / 2 - gap - p.width
                val y = ((trackPx - thumb.toPx()) * fraction + (thumb.toPx() - p.height) / 2f).roundToInt()
                layout(0, 0) { p.place(x, y) }
            }.mediaPlate(),
            SliverColors.OnMedia,
            maxLines = 1,
        )
    }
}
