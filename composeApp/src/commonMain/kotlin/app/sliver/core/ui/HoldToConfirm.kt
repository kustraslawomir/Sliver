package app.sliver.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val HOLD_TO_CONFIRM_MS = 1200L

/** Fraction 0..1 of a press-and-hold that has elapsed. */
fun holdProgress(elapsedMs: Long, durationMs: Long = HOLD_TO_CONFIRM_MS): Float =
    if (durationMs <= 0L) 1f else (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)

fun holdComplete(elapsedMs: Long, durationMs: Long = HOLD_TO_CONFIRM_MS): Boolean = elapsedMs >= durationMs

/**
 * Destructive confirmation: red text, a bar fills while held, fires on completion, cancels on release.
 * Reduced motion: no fill, only the timer. TalkBack reaches it as a long click.
 */
@Composable
fun HoldToConfirmButton(text: String, onConfirmed: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val reduced = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val fire by rememberUpdatedState(onConfirmed)
    var progress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    val color = if (enabled) SliverColors.Error else SliverColors.TextDisabled
    Box(
        modifier.heightIn(min = 48.dp).clip(SliverShapes.Pill)
            .border(1.dp, color, SliverShapes.Pill)
            .semantics {
                contentDescription = "$text. Press and hold"
                role = Role.Button
                if (enabled) onLongClick(label = text) { fire(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    var job: Job? = null
                    holding = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var done = false
                    job = scope.launch {
                        var elapsed = 0L
                        while (!holdComplete(elapsed)) {
                            delay(FRAME_MS)
                            elapsed += FRAME_MS
                            progress = holdProgress(elapsed)
                        }
                        done = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        fire()
                    }
                    tryAwaitRelease()
                    if (!done) job.cancel()
                    holding = false
                    progress = 0f
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!reduced && holding) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(SliverColors.Error.copy(alpha = 0.22f), Offset.Zero, Size(size.width * progress, size.height), style = Fill)
            }
        }
        SliverText(text, SliverType.Headline, Modifier.padding(horizontal = SliverSpace.S4), color = color, maxLines = 1, softWrap = false)
    }
}

private const val FRAME_MS = 16L
