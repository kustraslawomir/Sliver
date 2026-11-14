package app.sliver.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes

private const val SWEEP_MS = 1100
private const val SEGMENT = 0.35f

/** The one loading indicator: a thin line sweeping along a hairline track. Static under reduced motion. */
@Composable
fun SlitLoader(modifier: Modifier = Modifier, width: Dp = 120.dp) {
    val reduced = LocalReducedMotion.current
    // Perf STEP 2: the sweep is read in the layout phase (no recomposition per frame).
    val t: State<Float>? = if (reduced) {
        null
    } else {
        rememberInfiniteTransition(label = "slit-loader")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    }
    Box(modifier.width(width).height(2.dp).background(SliverColors.LineHairline, SliverShapes.Pill)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(SEGMENT)
                .layout { m, c ->
                    val p = m.measure(c)
                    val start = if (t == null) (1f - SEGMENT) / 2f else -SEGMENT + t.value * (1f + SEGMENT)
                    layout(p.width, p.height) { p.place((start * c.maxWidth / SEGMENT).toInt(), 0) }
                }
                .background(SliverColors.Accent, SliverShapes.Pill),
        )
    }
}
