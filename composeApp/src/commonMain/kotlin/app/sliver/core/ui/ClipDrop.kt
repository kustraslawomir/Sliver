package app.sliver.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/** Signature 2: a saved clip shrinks into the TODAY chip (Camera) or its calendar cell (Project). */
object ClipDrop {
    var pending by mutableStateOf<LocalDate?>(null)
    var target by mutableStateOf<Rect?>(null)

    fun clear() {
        pending = null
        target = null
    }
}

private const val TARGET_WAIT_MS = 500L

fun Modifier.clipDropTarget(date: LocalDate): Modifier = onGloballyPositioned {
    if (ClipDrop.pending == date) ClipDrop.target = it.boundsInRoot()
}

@Composable
fun ClipDropOverlay() {
    val date = ClipDrop.pending ?: return
    val target = ClipDrop.target
    val reduced = LocalReducedMotion.current
    val progress = remember(date) { Animatable(0f) }
    LaunchedEffect(date, target != null, reduced) {
        if (reduced) return@LaunchedEffect ClipDrop.clear()
        if (target == null) {
            delay(TARGET_WAIT_MS)
            return@LaunchedEffect ClipDrop.clear()
        }
        progress.animateTo(1f, SliverMotion.springDrop())
        ClipDrop.clear()
    }
    if (target == null || reduced) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() } * 0.45f
        val start = Rect(Offset((with(density) { maxWidth.toPx() } - w) / 2f, with(density) { maxHeight.toPx() } * 0.3f), Size(w, w * 16f / 9f))
        val p = progress.value
        val r = lerp(start, target, p)
        val corner = (16f + (6f - 16f) * p.coerceIn(0f, 1f)).coerceAtLeast(0f)
        val shape = RoundedCornerShape(corner.dp)
        Box(
            Modifier.offset { androidx.compose.ui.unit.IntOffset(r.left.toInt(), r.top.toInt()) }
                .size(with(density) { r.width.coerceAtLeast(0f).toDp() }, with(density) { r.height.coerceAtLeast(0f).toDp() })
                .alpha(0.6f + 0.4f * (1f - p.coerceIn(0f, 1f)))
                .clip(shape).background(SliverColors.Surface3).border(1.dp, SliverColors.LineStrong, shape),
        )
    }
}
