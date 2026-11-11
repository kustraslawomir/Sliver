package app.sliver.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverGlow
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import kotlinx.coroutines.delay

@Composable
fun SliverBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(SliverMotion.standard(SliverMotion.Slow)), exit = fadeOut(SliverMotion.exit(SliverMotion.Base))) {
            Box(
                Modifier.fillMaxSize().background(SliverColors.Scrim)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(SliverMotion.emphasized(SliverMotion.Slow)) { it },
            exit = slideOutVertically(SliverMotion.exit(SliverMotion.Base)) { it },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .then(SliverGlow.sheet(SliverShapes.SheetTop))
                    .clip(SliverShapes.SheetTop)
                    .background(SliverColors.Surface2)
                    .border(1.dp, SliverColors.LineHairline, SliverShapes.SheetTop)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .padding(horizontal = SliverSpace.Gutter)
                    .padding(top = SliverSpace.S3, bottom = SliverSpace.S6),
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp)
                        .clip(SliverShapes.Pill).background(SliverColors.LineStrong),
                )
                Box(Modifier.padding(top = SliverSpace.S4)) { content() }
            }
        }
    }
}

data class ToastMessage(val text: String, val actionLabel: String = "Undo", val onAction: (() -> Unit)? = null)

@Stable
class UndoToastState {
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    fun show(text: String, actionLabel: String = "Undo", onAction: (() -> Unit)? = null) {
        current = ToastMessage(text, actionLabel, onAction)
    }

    fun dismiss() {
        current = null
    }

    fun performAction() {
        current?.onAction?.invoke()
        current = null
    }
}

@Composable
fun rememberUndoToastState() = remember { UndoToastState() }

@Composable
fun UndoToastHost(state: UndoToastState, modifier: Modifier = Modifier, durationMs: Long = 4000) {
    val msg = state.current
    LaunchedEffect(msg) {
        if (msg != null) {
            delay(durationMs)
            if (state.current === msg) state.dismiss()
        }
    }
    Box(modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(msg != null, enter = fadeIn(SliverMotion.standard()), exit = fadeOut(SliverMotion.exit())) {
            val shown = msg ?: return@AnimatedVisibility
            Row(
                Modifier.fillMaxWidth().height(56.dp)
                    .then(SliverGlow.sheet(SliverShapes.Pill))
                    .clip(SliverShapes.Pill)
                    .background(SliverColors.Surface2)
                    .border(1.dp, SliverColors.LineHairline, SliverShapes.Pill)
                    .padding(horizontal = SliverSpace.S5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliverText(shown.text, SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextPrimary, maxLines = 1)
                if (shown.onAction != null) {
                    SliverText(
                        shown.actionLabel, SliverType.Headline,
                        Modifier.clickable(onClick = state::performAction).padding(SliverSpace.S2),
                        SliverColors.Accent,
                    )
                }
            }
        }
    }
}

enum class SliverFrameState { Idle, Dragging, Locked }

/** Static visual of the trim window; caller sizes it (1 s of strip wide). */
@Composable
fun SliverFrame(state: SliverFrameState, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (state == SliverFrameState.Dragging) 1.04f else 1f, SliverMotion.springLockIn())
    val borderWidth by animateFloatAsState(
        if (state == SliverFrameState.Locked) 3f else 2.5f,
        if (reduced) SliverMotion.standard(SliverMotion.Instant) else SliverMotion.standard(SliverMotion.Slow),
    )
    val color = if (state == SliverFrameState.Dragging) SliverColors.AccentCore else SliverColors.Accent
    Box(
        modifier.scale(scale)
            .then(SliverGlow.medium(SliverShapes.S))
            .border(borderWidth.dp, color, SliverShapes.S),
    )
}

/** Dims everything outside the frame region of a strip to 66%. */
@Composable
fun StripDim(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Box(modifier.background(color.copy(alpha = 0.66f)))
}
