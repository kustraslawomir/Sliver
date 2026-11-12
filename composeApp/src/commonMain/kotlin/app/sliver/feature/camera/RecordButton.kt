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
