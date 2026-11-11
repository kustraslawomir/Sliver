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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.sliver.core.designsystem.Glow
import app.sliver.core.designsystem.glow
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

private data class FocusState(val x: Float, val y: Float, val tap: Int, val locked: Boolean = false)

/**
 * Tap-to-focus layer. Sits under the camera controls, so taps on controls never reach it.
 * Tap: AF+AE ring (88 -> 64 dp, 220 ms, fades to 35% after 1.2 s). Hold 600 ms: AE/AF lock.
 */
@Composable
fun FocusOverlay(enabled: Boolean, onFocus: (x: Float, y: Float, lock: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    var focus by remember { mutableStateOf<FocusState?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(enabled) { if (!enabled) focus = null }

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
                    onFocus(down.position.x, down.position.y, false)
                    val up = withTimeoutOrNull(FOCUS_LOCK_HOLD_MS) { waitForUpOrCancellation() }
                    if (up == null) {
                        focus = FocusState(cx, cy, tapId, locked = true)
                        onFocus(down.position.x, down.position.y, true)
                        waitForUpOrCancellation()
                    }
                }
            },
    ) {
        val f = focus ?: return@Box
        FocusRing(f, reduced, Offset(f.x, f.y))
    }
}

@Composable
private fun FocusRing(f: FocusState, reduced: Boolean, center: Offset) {
    val ringSize = remember(f.tap) { Animatable(if (reduced) FOCUS_RING_END_DP.toFloat() else FOCUS_RING_START_DP.toFloat()) }
    val opacity = remember(f.tap) { Animatable(1f) }
    LaunchedEffect(f.tap) {
        if (!reduced) ringSize.animateTo(FOCUS_RING_END_DP.toFloat(), tween(220, easing = SliverMotion.EaseStandard))
    }
    LaunchedEffect(f.tap, f.locked) {
        if (f.locked) opacity.snapTo(1f) else {
            delay(FOCUS_DIM_AFTER_MS)
            opacity.animateTo(0.35f, tween(400))
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
