package app.sliver.feature.camera
import androidx.compose.runtime.derivedStateOf
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
 * T-record-button-v3 (owner 14 Nov: simple — white ring + flat red disc; replaces the look of T-glass-v1.7 items 4 and 7,
 * keeps their behaviour). One Canvas in a 92 dp box: white ring → flat red disc 76 dp (morphs to a 32 dp square, corner
 * 8 dp, while recording) → 1 s progress arc on the ring. A timer pill floats above while recording. No glow, no gloss, no
 * looping animation (nothing runs while the camera is idle or paused).
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
    elapsedMs: () -> Long = { 0L },
) {
    app.sliver.core.log.RecomposeProbe("recordButton")
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

    Box(
        modifier.size(Box92)
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
        Canvas(Modifier.size(Box92).alpha(if (enabled) 1f else 0.5f)) {
            val progress = if (recording) (elapsedMs() / 1000f).coerceIn(0f, 1f) else 0f
            drawRecordButton(white = white, rec = rec, press = press, progress = progress, flash = flash.value)
        }
        if (recording) {
            RecordTimerPill(elapsedMs, Modifier.align(Alignment.TopCenter).wrapContentSize(unbounded = true).offset(y = -(30.dp + 30.dp)))
        }
    }
}

private fun DrawScope.drawRecordButton(white: Boolean, rec: Float, press: Float, progress: Float, flash: Float) {
    val c = center
    val ringW = 4.dp.toPx()
    val ringR = 46.dp.toPx() - ringW / 2f
    // A faint dark edge keeps the white ring readable over a bright scene.
    drawCircle(Color.Black.copy(alpha = 0.18f), ringR + ringW / 2f + 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
    // While recording the ring dims and the progress arc fills it in white.
    drawCircle(Color.White.copy(alpha = lerp(1f, 0.35f, rec)), ringR, c, style = Stroke(ringW))
    if (progress > 0f) {
        drawArc(
            Color.White, -90f, 360f * progress, false, c - Offset(ringR, ringR), Size(2 * ringR, 2 * ringR),
            style = Stroke(ringW, cap = StrokeCap.Round),
        )
    }
    // Flat disc 76 dp → 32 dp square (corner 8 dp); pressed it sinks to 92 %.
    val k = 1f - 0.08f * press
    val side = lerp(76.dp.toPx(), 32.dp.toPx(), rec) * k
    val corner = lerp(side / 2f, 8.dp.toPx() * k, rec)
    drawRoundRect(
        if (white) SliverColors.PhotoCore else SliverColors.Record,
        c - Offset(side / 2f, side / 2f), Size(side, side), CornerRadius(corner),
    )
    // Saved: the ring flashes wider for a moment.
    if (flash > 0f) {
        val w = lerp(ringW, 9.dp.toPx(), flash)
        drawCircle(Color.White.copy(alpha = flash), 46.dp.toPx() - w / 2f, c, style = Stroke(w))
    }
}

private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

/** "0.6 s" over black 25 % glass with the red recording dot. */
@Composable
private fun RecordTimerPill(elapsedMs: () -> Long, modifier: Modifier = Modifier) {
    val tenths by remember { derivedStateOf { (elapsedMs() / 100).toInt() } }
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
