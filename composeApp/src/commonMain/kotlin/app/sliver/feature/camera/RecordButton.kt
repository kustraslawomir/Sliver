package app.sliver.feature.camera

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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

/** CSS ease-in-out, as in design sv-breathe (1600 ms). */
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private const val BREATH_LOW = 0.78f
private val Disc = 72.dp

/** 72 dp red disc in an 80 dp hit area; while recording it breathes in opacity 1 to 0.78 over 1.6 s (static with reduced motion). */
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
    val alpha = if (state == RecordState.Recording && !reduced) {
        val t by rememberInfiniteTransition(label = "breath").animateFloat(
            1f, BREATH_LOW, infiniteRepeatable(tween(BREATH_MS / 2, easing = EaseInOut), RepeatMode.Reverse), label = "breath",
        )
        t
    } else 1f
    val white = state == RecordState.Photo
    val base = if (white) SliverColors.TextPrimary else SliverColors.Record
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
        val phase = if (reduced) null else rememberInfiniteTransition(label = "liquid").animateFloat(
            0f, 1f, infiniteRepeatable(tween(LIQUID_MS, easing = LinearEasing)), label = "liquid",
        )
        val strength = animateFloatAsState(
            when { !enabled -> 0f; state == RecordState.Recording -> 1f; else -> 0.6f }, tween(450), label = "glow",
        )
        Box(Modifier.size(80.dp).liquidGlow(phase, strength, if (white) WhiteGlow else RedGlow))
        Box(
            Modifier.size(Disc).alpha(alpha).background(base, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state == RecordState.Recording) {
                Box(Modifier.size(24.dp).background(Color.White, RoundedCornerShape(6.dp)))
            }
        }
    }
}

private const val LIQUID_MS = 12_000
private val RedGlow = listOf(Color(0xFFFF3B30), Color(0xFFFF7A45), Color(0xFFFF2D7A))
private val WhiteGlow = listOf(Color(0xFFFFFFFF), Color(0xFFBFD4FF), Color(0xFFFFE9D6))

/**
 * Soft light that flows around the button: three radial blobs drift on looping orbits and are added to the picture
 * (BlendMode.Plus), so where they overlap the colours mix like liquid light. Whole-number speeds make the loop seamless.
 * [phase] null (reduced motion) = a still glow.
 */
private fun Modifier.liquidGlow(phase: State<Float>?, strength: State<Float>, colors: List<Color>): Modifier = drawBehind {
    val s = strength.value
    if (s <= 0f) return@drawBehind
    val p = (phase?.value ?: 0.1f) * 2f * PI.toFloat()
    val base = size.minDimension / 2f
    colors.forEachIndexed { i, color ->
        val k = i + 1
        val orbit = base * (0.22f + 0.08f * sin(p * 2 + i))
        val c = Offset(center.x + cos(p * k + i * 2.1f) * orbit, center.y + sin(p * (4 - k) + i * 1.3f) * orbit)
        val r = base * (1.75f + 0.25f * sin(p * 2 + i * 2f)) * (0.85f + 0.15f * s)
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.50f * s), 0.45f to color.copy(alpha = 0.20f * s), 1f to Color.Transparent,
                center = c, radius = r,
            ),
            radius = r, center = c, blendMode = BlendMode.Plus,
        )
    }
}
