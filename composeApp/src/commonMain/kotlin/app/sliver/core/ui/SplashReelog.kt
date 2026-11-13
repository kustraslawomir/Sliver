package app.sliver.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.ReelogWordmark
import app.sliver.core.icons.WORDMARK_DISC_Y
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.theme.generated.Tokens
import kotlinx.coroutines.delay

/** Centre of the Record today disc above the navigation bar: 12 bottom padding + 20 label + 8 gap + 36 radius. */
private const val DOCK_DISC_FROM_BOTTOM_DP = 76f
private const val MAX_STEP_MS = 34f

/**
 * Cold-start splash (INDEX v1.6): the system splash disc shrinks into the "o" of "reel●g", holds with the tagline,
 * then grows back and lands on the Record today button while the layer fades. [toProject] null = still loading;
 * false (onboarding first) = plain fade. Always dark.
 */
@Composable
fun SplashOverlay(toProject: Boolean?, onFinished: () -> Unit) {
    val reduced = LocalReducedMotion.current
    var t by remember { mutableFloatStateOf(SPLASH_START_MS.toFloat()) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(toProject) {
        val project = toProject ?: return@LaunchedEffect
        when {
            !project -> fade.animateTo(0f, SliverMotion.standard(SPLASH_FADE_MS))
            reduced -> {
                t = 1000f
                delay(SPLASH_REDUCED_HOLD_MS.toLong())
                fade.animateTo(0f, SliverMotion.standard(SPLASH_FADE_MS))
            }
            else -> {
                // The first frames of the screen underneath are slow; a frame never advances the clock by more than
                // MAX_STEP_MS, so the sequence slows down a little instead of skipping.
                var last = withFrameNanos { it }
                while (t < SPLASH_END_MS) {
                    val now = withFrameNanos { it }
                    t = (t + ((now - last) / 1_000_000f).coerceAtMost(MAX_STEP_MS)).coerceAtMost(SPLASH_END_MS.toFloat())
                    last = now
                }
            }
        }
        onFinished()
    }
    val reel = remember { PathParser().parsePathString(ReelogWordmark.REEL).toPath() }
    val g = remember { PathParser().parsePathString(ReelogWordmark.G).toPath() }
    val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        val centerY = maxHeight.value / 2
        val f = splashFrame(t, centerY, maxHeight.value - navBottom.value - DOCK_DISC_FROM_BOTTOM_DP)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Tokens.DarkColors.bgBase, alpha = f.background)
            val k = SPLASH_WORD_SCALE.dp.toPx()
            val left = size.width / 2 - ReelogWordmark.WIDTH / 2 * k
            val top = centerY.dp.toPx() - WORDMARK_DISC_Y * k
            if (f.letters > 0f) {
                translate(left + f.reelShift * k, top) {
                    scale(k, Offset.Zero) { drawPath(reel, Tokens.DarkColors.textPrimary, alpha = f.letters) }
                }
                translate(left + f.gShift * k, top) {
                    scale(k, Offset.Zero) { drawPath(g, Tokens.DarkColors.textPrimary, alpha = f.letters) }
                }
            }
            drawCircle(
                Tokens.RecordRed,
                radius = f.discSize.dp.toPx() / 2,
                center = Offset(size.width / 2 + f.discDx.dp.toPx(), f.discY.dp.toPx()),
            )
        }
        Box(Modifier.fillMaxWidth().offset(y = (centerY + SPLASH_TAGLINE_GAP_DP).dp).graphicsLayer { alpha = f.tagline }) {
            SliverText(
                "One clip a day",
                SliverType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp, textAlign = TextAlign.Center),
                color = Tokens.DarkColors.textTertiary,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }
    }
}
