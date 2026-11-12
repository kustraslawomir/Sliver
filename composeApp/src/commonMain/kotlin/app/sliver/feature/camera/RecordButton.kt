package app.sliver.feature.camera

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RecordState { Photo, VideoIdle, Recording }

private const val BREATH_MS = 1600

/** CSS ease-in-out, as in design sv-breathe (1600 ms, 0%/100% = 12 px, 50% = 18 px). */
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val Disc = 72.dp

/** Option D "Ember": 72 dp disc in an 80 dp hit area, glow (tokens glow.record, breathing to recordBreathPeak) drawn as a radial gradient (no blur). */
@Composable
fun RecordButton(
    state: RecordState,
    holding: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val breath = if (state == RecordState.Recording && !reduced) {
        val t by rememberInfiniteTransition(label = "breath").animateFloat(
            0f, 1f, infiniteRepeatable(tween(BREATH_MS / 2, easing = EaseInOut), RepeatMode.Reverse), label = "breath",
        )
        t
    } else 0f
    val white = state == RecordState.Photo
    val base = if (white) SliverColors.TextPrimary else SliverColors.Accent
    val inner = if (white) 0x33 / 255f else lerpF(0x4D / 255f, 0x73 / 255f, breath)
    val reach = if (white) 12f else lerpF(12f, 18f, breath)
    Box(
        modifier.size(80.dp).scale(if (holding) 1.2f else 1f)
            .semantics { contentDescription = if (state == RecordState.Recording) "Stop recording" else "Record"; role = Role.Button }
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
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(Disc)
                .drawBehind {
                    val r = size.minDimension / 2f
                    val reachPx = reach.dp.toPx()
                    val edge = r / (r + reachPx)
                    val mid = (r + reachPx * 0.45f) / (r + reachPx)
                    drawCircle(
                        Brush.radialGradient(
                            edge to base.copy(alpha = inner),
                            mid to base.copy(alpha = inner * 0.4f),
                            1f to Color.Transparent,
                            center = Offset(r, r), radius = r + reachPx,
                        ),
                        radius = r + reachPx, center = Offset(r, r),
                    )
                }
                .background(base, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state == RecordState.Recording) {
                Box(Modifier.size(24.dp).background(SliverColors.OnAccent, RoundedCornerShape(6.dp)))
            }
        }
    }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
