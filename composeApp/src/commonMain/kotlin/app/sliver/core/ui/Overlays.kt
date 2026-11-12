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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
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
    var back by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) { if (visible) back = 0f }
    PredictiveBackHandler(visible, { back = it }, onDismiss)
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
                Modifier.widthIn(max = MaxContentWidth).fillMaxWidth()
                    .graphicsLayer {
                        val scale = 1f - 0.1f * back
                        scaleX = scale
                        scaleY = scale
                        translationY = back * 24.dp.toPx()
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .then(Modifier.shadow(24.dp, SliverShapes.SheetTop, ambientColor = Color.Black, spotColor = Color.Black)) // lint-ok: elevation
                    .clip(SliverShapes.SheetTop)
                    .background(SliverColors.Surface2)
                    .border(1.dp, SliverColors.LineHairline, SliverShapes.SheetTop)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
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
            .border(borderWidth.dp, color, SliverShapes.S),
    )
}

/** Dims everything outside the frame region of a strip to 66%. */
@Composable
fun StripDim(modifier: Modifier = Modifier, color: Color = SliverColors.Media) {
    Box(modifier.background(color.copy(alpha = 0.66f)))
}
