package app.sliver.feature.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import app.sliver.core.designsystem.Glow
import app.sliver.core.designsystem.glow
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

private data class FocusState(val x: Float, val y: Float, val tap: Int, val locked: Boolean = false, val ok: Boolean? = null)

/**
 * Tap-to-focus layer. It sits over the preview and under the camera controls, so its coordinates are PreviewView
 * coordinates and taps on controls never reach it.
 * Tap: AF+AE ring (88 -> 64 dp, 220 ms, dims to 35% after 1.2 s or at once if focus failed).
 * Hold 600 ms: AE/AF lock. A vertical exposure slider appears beside the ring.
 */
@Composable
fun FocusOverlay(
    enabled: Boolean,
    resetKey: Any,
    exposureRange: () -> ExposureRange?,
    onFocus: (x: Float, y: Float, lock: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    onExposure: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
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
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val (cx, cy) = focusRingCenter(
                        down.position.x, down.position.y, size.width.toFloat(), size.height.toFloat(), with(density) { FOCUS_RING_START_DP.dp.toPx() },
                    )
                    val tapId = (focus?.tap ?: 0) + 1
                    focus = FocusState(cx, cy, tapId)
                    range = exposureRange()
                    interaction++
                    onFocus(down.position.x, down.position.y, false) { ok ->
                        if (focus?.tap == tapId) focus = focus?.copy(ok = ok)
                    }
                    val up = withTimeoutOrNull(FOCUS_LOCK_HOLD_MS) { waitForUpOrCancellation() }
                    if (up == null) {
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
        FocusRing(f, reduced, Offset(f.x, f.y))
        val r = range
        if (r != null && enabled) {
            ExposureSlider(
                visible = sliderVisible || f.locked, locked = f.locked, range = r, index = evIndex,
                centerX = f.x, centerY = f.y, width = size.width.toFloat(), height = size.height.toFloat(),
                onIndex = { if (it != evIndex) { evIndex = it; onExposure(it) } },
                onTouch = { interaction++ },
            )
        }
    }
}

@Composable
private fun FocusRing(f: FocusState, reduced: Boolean, center: Offset) {
    val ringSize = remember(f.tap) { Animatable(if (reduced) FOCUS_RING_END_DP.toFloat() else FOCUS_RING_START_DP.toFloat()) }
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
    val color = if (f.locked) SliverColors.Accent else Color.White
    val d = ringSize.value
    Box(Modifier.fillMaxSize().alpha(opacity.value)) {
        Box(
            Modifier.offset { IntOffset((center.x - d.dp.toPx() / 2).roundToInt(), (center.y - d.dp.toPx() / 2).roundToInt()) }
                .size(d.dp)
                .then(if (f.locked) Modifier.glow(CircleShape, *Glow.lock, stroke = 1.5.dp) else Modifier)
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(4.dp).background(color, CircleShape))
        }
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
                    "AE/AF LOCK", SliverType.MonoS.copy(fontSize = TextUnit(10f, TextUnitType.Sp)),
                    Modifier.background(SliverColors.Accent, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    SliverColors.OnAccent, maxLines = 1,
                )
            }
        }
    }
}

private val SliderWidth = 32.dp
private val SliderGap = 52.dp

/** Neutral white vertical slider beside the focus ring; ember only while AE/AF is locked. */
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
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "ev-fade")
    if (alpha == 0f) return
    var dragging by remember { mutableStateOf(false) }
    val trackPx = with(density) { EXPOSURE_TRACK_DP.dp.toPx() }
    val onRight = exposureOnRight(centerX, width)
    val gapPx = with(density) { SliderGap.toPx() }
    val widthPx = with(density) { SliderWidth.toPx() }
    val left = if (onRight) centerX + gapPx - widthPx / 2 else centerX - gapPx - widthPx / 2
    val top = (centerY - trackPx / 2).coerceIn(0f, (height - trackPx).coerceAtLeast(0f))
    val fraction = exposureFraction(index, range.min, range.max)
    val tint = if (locked) SliverColors.Accent else Color.White
    Box(
        Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .width(SliderWidth).height(EXPOSURE_TRACK_DP.dp).alpha(alpha)
            .semantics { contentDescription = "Brightness ${formatEv(index, range.step)}" }
            .pointerInput(range) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    dragging = true
                    onTouch()
                    onIndex(exposureIndexAt(down.position.y / size.height, range.min, range.max))
                    verticalDrag(down.id) { change ->
                        change.consume()
                        onTouch()
                        onIndex(exposureIndexAt(change.position.y / size.height, range.min, range.max))
                    }
                    dragging = false
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.align(Alignment.Center).width(2.dp).fillMaxSize().background(Color.White.copy(alpha = 0.6f), CircleShape))
        val thumb = 28.dp
        Box(
            Modifier.offset { IntOffset(0, ((trackPx - with(density) { thumb.toPx() }) * fraction).roundToInt()) }
                .size(thumb).background(Color.Black.copy(alpha = 0.55f), CircleShape).border(1.dp, tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { SliverIcon(SliverIcons.Sun, tint, size = 16.dp) }
        if (dragging) {
            SliverText(
                formatEv(index, range.step), SliverType.MonoS.copy(fontSize = TextUnit(10f, TextUnitType.Sp)),
                Modifier.align(if (onRight) Alignment.TopEnd else Alignment.TopStart)
                    .offset { IntOffset(0, ((trackPx - with(density) { 28.dp.toPx() }) * fraction).roundToInt() - with(density) { 18.dp.toPx() }.roundToInt()) },
                Color.White, maxLines = 1,
            )
        }
    }
}
