package app.sliver.core.ui

import app.sliver.core.designsystem.Glow
import app.sliver.core.designsystem.glow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes

private const val SWEEP_MS = 1100
private const val SEGMENT = 0.35f

/** The one loading indicator: a thin ember line sweeping along a hairline track. Static under reduced motion. */
@Composable
fun SlitLoader(modifier: Modifier = Modifier, width: Dp = 120.dp) {
    val reduced = LocalReducedMotion.current
    val t = if (reduced) 0f else {
        val transition = rememberInfiniteTransition(label = "slit-loader")
        val v by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
        v
    }
    val start = if (reduced) (1f - SEGMENT) / 2f else -SEGMENT + t * (1f + SEGMENT)
    Box(modifier.width(width).height(2.dp).background(SliverColors.LineHairline, SliverShapes.Pill)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(SEGMENT)
                .layout { m, c ->
                    val p = m.measure(c)
                    layout(p.width, p.height) { p.place((start * c.maxWidth / SEGMENT).toInt(), 0) }
                }
                .glow(SliverShapes.Pill, *Glow.slit)
                .background(SliverColors.Accent, SliverShapes.Pill),
        )
    }
}
