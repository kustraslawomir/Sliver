package app.sliver.feature.camera
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.ReelogType
import app.sliver.core.kit.glass
import app.sliver.core.kit.GlassStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

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

private val Box92 = 92.dp

/**
 * v1.7 record button (T-glass-v1.7 item 7, design/motion/RecordButton.html frame(t)). One Canvas, bottom to top:
 * liquid glow → strong-glass disc 92 dp → outer rim → machined gap ring 72 dp → liquid red core 64 dp (morphs to a
 * 30 dp rounded square while recording) → 1 s progress arc. A timer pill floats 30 dp above while recording.
 * Glass here is tint only: the camera preview is a SurfaceView the blur cannot read.
 */
@Composable
fun RecordButton(
    state: RecordState,
    holding: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    elapsedMs: Long = 0L,
) {
    val reduced = LocalReducedMotion.current
    val recording = state == RecordState.Recording
    val white = state == RecordState.Photo
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")
    // Recording: spring with a little overshoot (~420 ms); back to the disc: 460 ms ease-in-out.
    val rec by animateFloatAsState(
        if (recording) 1f else 0f,
        if (recording) spring(dampingRatio = 0.55f, stiffness = 260f) else tween(460, easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)),
        label = "rec",
    )
    // Saved: white ring flash 8 dp → 2 dp over 480 ms.
    val flash = remember { Animatable(0f) }
    var wasRecording by remember { mutableStateOf(false) }
    LaunchedEffect(recording) {
        if (wasRecording && !recording) { flash.snapTo(1f); flash.animateTo(0f, tween(480)) }
        wasRecording = recording
    }
    val loop = rememberInfiniteTransition(label = "record")
    val phase = if (reduced) null else loop.animateFloat(0f, 1f, infiniteRepeatable(tween(LIQUID_MS, easing = LinearEasing)), label = "glow")
    val spin = if (reduced) null else loop.animateFloat(0f, 1f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "spin")
    val strength = animateFloatAsState(when { !enabled -> 0f; recording -> 1f; else -> 0.6f }, tween(450), label = "glow")
    val progress = if (recording) (elapsedMs / 1000f).coerceIn(0f, 1f) else 0f

    Box(
        modifier.size(Box92)
            .graphicsLayer { val k = 1f - 0.04f * press; scaleX = k; scaleY = k }
            .semantics { contentDescription = if (recording) "Stop recording" else "Record"; role = Role.Button }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    coroutineScope {
                        pressed = true
                        var held = false
                        val job = launch {
                            delay(HOLD_THRESHOLD_MS)
                            held = true
                            onHoldStart()
                        }
                        val released = tryAwaitRelease()
                        pressed = false
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
        Box(Modifier.size(Box92).liquidGlow(phase, strength, if (white) SliverColors.RecordGlowWhite else SliverColors.RecordGlowRed))
        Canvas(Modifier.size(Box92)) {
            drawRecordButton(
                white = white, rec = rec, press = press, progress = progress, flash = flash.value,
                rimTurn = (phase?.value ?: 0f), spin = spin?.value ?: 0f,
            )
        }
        if (recording) {
            RecordTimerPill(elapsedMs, Modifier.align(Alignment.TopCenter).wrapContentSize(unbounded = true).offset(y = -(30.dp + 30.dp)))
        }
    }
}

private fun DrawScope.drawRecordButton(white: Boolean, rec: Float, press: Float, progress: Float, flash: Float, rimTurn: Float, spin: Float) {
    val c = center
    val outer = 46.dp.toPx()
    // Shadow 0/14/34 black 45 %.
    drawCircle(
        Brush.radialGradient(0.6f to Color.Black.copy(alpha = 0.45f), 1f to Color.Transparent, center = c + Offset(0f, 14.dp.toPx()), radius = outer + 17.dp.toPx()),
        radius = outer + 17.dp.toPx(), center = c + Offset(0f, 14.dp.toPx()),
    )
    // Strong glass disc: white 10 % over a dark base (no backdrop to blur over the camera surface).
    drawCircle(Color.Black.copy(alpha = 0.22f), outer, c)
    drawCircle(Color.White.copy(alpha = 0.10f), outer, c)
    // Outer rim: white at the top, faint at the sides, warm red at the bottom; sways ±6° with the glow.
    val sway = 6f * sin(rimTurn * 2f * PI.toFloat())
    rotate(sway, c) {
        drawCircle(
            Brush.sweepGradient(
                0f to Color.White.copy(alpha = 0.15f), 0.25f to (if (white) Color.White.copy(alpha = 0.4f) else SliverColors.RecordRimWarm),
                0.5f to Color.White.copy(alpha = 0.15f), 0.75f to Color.White.copy(alpha = 0.95f), 1f to Color.White.copy(alpha = 0.15f),
                center = c,
            ),
            outer - 0.3.dp.toPx(), c, style = Stroke(0.6.dp.toPx()),
        )
    }
    // Machined gap ring: 72 dp, black 55 %, 1 dp inner shadow, inner rim white 45 % top/bottom.
    val gap = 36.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.55f), gap, c)
    drawCircle(Color.Black.copy(alpha = 0.6f), gap - 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
    drawCircle(
        Brush.sweepGradient(
            0f to Color.Transparent, 0.25f to Color.White.copy(alpha = 0.45f), 0.5f to Color.Transparent,
            0.75f to Color.White.copy(alpha = 0.45f), 1f to Color.Transparent, center = c,
        ),
        gap - 0.3.dp.toPx(), c, style = Stroke(0.6.dp.toPx()),
    )
    // Core: 64 dp disc → 30 dp square (corner 9 dp) while recording; ×0.92 on press.
    val k = 1f - 0.08f * press
    val side = lerp(64.dp.toPx(), 30.dp.toPx(), rec) * k
    val corner = lerp(side / 2f, 9.dp.toPx() * k, rec)
    val topLeft = c - Offset(side / 2f, side / 2f)
    val shape = Path().apply { addRoundRect(RoundRect(Rect(topLeft, Size(side, side)), CornerRadius(corner))) }
    clipPath(shape) {
        if (white) {
            drawRect(Brush.verticalGradient(listOf(Color.White, SliverColors.PhotoCoreLow), topLeft.y, topLeft.y + side), topLeft, Size(side, side))
        } else {
            drawRect(SliverColors.RecordCoreBase, topLeft, Size(side, side))
            // Liquid: soft red blobs orbiting once every 5 s (the HTML blurs a sweep by 7 dp; a canvas can't, so the
            // blobs are radial gradients, which read the same without the cone seams).
            val turn = spin * 2f * PI.toFloat()
            SliverColors.RecordLiquid.take(5).forEachIndexed { i, color ->
                val a2 = turn + i * (2f * PI.toFloat() / 5f)
                val p = c + Offset(cos(a2) * side * 0.28f, sin(a2) * side * 0.28f)
                drawCircle(Brush.radialGradient(0f to color, 1f to color.copy(alpha = 0f), center = p, radius = side * 0.55f), side * 0.55f, p)
            }
            // Subsurface glow at the bottom.
            drawRect(
                Brush.radialGradient(
                    0f to SliverColors.RecordSubsurface, 1f to Color.Transparent,
                    center = Offset(c.x, topLeft.y + side * 1.05f), radius = side * 0.6f,
                ),
                topLeft, Size(side, side),
            )
        }
        // Gloss: ellipse 68 × 44 % at the top.
        val gw = side * 0.68f
        val gh = side * 0.44f
        drawOval(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.8f), 0.5f to Color.White.copy(alpha = 0.18f), 1f to Color.Transparent,
                startY = topLeft.y + side * 0.05f, endY = topLeft.y + side * 0.05f + gh,
            ),
            Offset(topLeft.x + side * 0.16f, topLeft.y + side * 0.05f), Size(gw, gh),
        )
        // Edges: dark inner shadow at the bottom (red core only), thin highlight at the top.
        if (!white) drawRect(
            Brush.verticalGradient(0.7f to Color.Transparent, 1f to SliverColors.RecordEdge, startY = topLeft.y, endY = topLeft.y + side),
            topLeft, Size(side, side),
        )
    }
    drawPath(
        shape,
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.45f), 0.2f to Color.Transparent, startY = topLeft.y, endY = topLeft.y + side),
        style = Stroke(1.5.dp.toPx()),
    )
    // Progress arc on the glass ring: r 41 dp, 3 dp white, round caps, soft glow.
    if (progress > 0f) {
        val r = 41.dp.toPx()
        val arcTopLeft = c - Offset(r, r)
        drawArc(Color.White.copy(alpha = 0.25f), -90f, 360f * progress, false, arcTopLeft, Size(2 * r, 2 * r), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color.White, -90f, 360f * progress, false, arcTopLeft, Size(2 * r, 2 * r), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
    // Saved: white ring flash 8 dp → 2 dp.
    if (flash > 0f) {
        val w = lerp(2.dp.toPx(), 8.dp.toPx(), flash)
        drawCircle(Color.White.copy(alpha = flash), outer - w / 2f, c, style = Stroke(w))
    }
}

private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

/** "0.6 s" over black 25 % glass with the red recording dot. */
@Composable
private fun RecordTimerPill(elapsedMs: Long, modifier: Modifier = Modifier) {
    val tenths = (elapsedMs / 100).toInt()
    Row(
        modifier.height(30.dp).glass(GlassStyle.Regular, RoundedCornerShape(15.dp), overMedia = true).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(7.dp).background(SliverColors.Record, CircleShape))
        SliverText("${tenths / 10}.${tenths % 10} s", ReelogType.label.copy(fontFeatureSettings = "tnum"), color = SliverColors.OnMedia, maxLines = 1)
    }
}

private const val LIQUID_MS = 12_000

/**
 * Soft light that flows around the button: three radial blobs drift on looping orbits and are added to the picture
 * (BlendMode.Plus), so where they overlap the colours mix like liquid light. Whole-number speeds make the loop seamless.
 * [phase] null (reduced motion) = a still glow.
 */
internal fun Modifier.liquidGlow(phase: State<Float>?, strength: State<Float>, colors: List<Color>): Modifier = drawBehind {
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
