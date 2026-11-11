package app.sliver.feature.camera

import app.sliver.data.FilmFormat
import app.sliver.data.LookPreset
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val HOLD_THRESHOLD_MS = 300L

val SelfTimerOptions = listOf(0, 3, 10)

fun nextSelfTimer(current: Int): Int =
    SelfTimerOptions[(SelfTimerOptions.indexOf(current) + 1) % SelfTimerOptions.size]

const val CINEMA_ASPECT = 16f / 9f
const val TURN_HINT_MS = 3500L

fun FilmFormat.label(): String = if (this == FilmFormat.CINEMA) "Cinema 16:9" else "Story 9:16"

/** Device is held upright (portrait, either way up) given OrientationEventListener-style degrees. */
fun isUpright(rotation: Int): Boolean {
    val r = ((rotation % 360) + 360) % 360
    return r < 45 || r >= 315 || r in 135..224
}

/** Snaps degrees to 0/90/180/270 and returns the counter-rotation to apply to icons. */
fun iconRotation(rotation: Int): Float {
    val snapped = (((rotation % 360) + 360) % 360 + 45) / 90 % 4 * 90
    return if (snapped == 0) 0f else -snapped.toFloat()
}

/** Height of the 16:9 band as a fraction of a portrait frame of [width] x [height]. */
fun cinemaBandFraction(width: Float, height: Float): Float =
    if (width <= 0f || height <= 0f) 1f else (width / CINEMA_ASPECT / height).coerceAtMost(1f)

/** 0..1 crop offset (0 = band at top) for a band of [bandFraction] whose top sits at [topFraction]. */
fun cropYFromTop(topFraction: Float, bandFraction: Float): Float {
    val room = 1f - bandFraction
    return if (room <= 0f) 0.5f else (topFraction / room).coerceIn(0f, 1f)
}

fun topFromCropY(cropY: Float, bandFraction: Float): Float = cropY.coerceIn(0f, 1f) * (1f - bandFraction)

/** Per-process flag so the turn-sideways hint appears once per session. */
object TurnHintSession {
    var shown = false
}

/** m:ss, e.g. 0:07. */
fun formatTimer(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** Surface.ROTATION_* value (0, 1, 2, 3 = 0, 90, 180, 270) for OrientationEventListener degrees. */
fun surfaceRotation(degrees: Int): Int {
    val d = ((degrees % 360) + 360) % 360
    return when {
        d >= 315 || d < 45 -> 0
        d < 135 -> 3
        d < 225 -> 2
        else -> 1
    }
}

/** Size a player shows for a stored [width] x [height] video with container [rotationDegrees] metadata. */
fun displaySize(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> =
    if (rotationDegrees % 180 == 0) width to height else height to width

const val FOCUS_LOCK_HOLD_MS = 600L
const val FOCUS_DIM_AFTER_MS = 1200L
const val FOCUS_AUTO_CANCEL_S = 3L
const val FOCUS_RING_START_DP = 88
const val FOCUS_RING_END_DP = 64
const val FOCUS_LOCK_PILL_OFFSET_DP = 54

/** Tap position inside the viewfinder, clamped to its bounds, as 0..1 fractions. */
fun focusFraction(x: Float, y: Float, width: Float, height: Float): Pair<Float, Float> =
    if (width <= 0f || height <= 0f) 0.5f to 0.5f
    else (x / width).coerceIn(0f, 1f) to (y / height).coerceIn(0f, 1f)

/** Ring is not drawn past the viewfinder edge: keeps the centre of a ring of [ringDp] fully visible. */
fun focusRingCenter(x: Float, y: Float, width: Float, height: Float, ringPx: Float): Pair<Float, Float> {
    val half = ringPx / 2f
    return x.coerceIn(half.coerceAtMost(width / 2f), (width - half).coerceAtLeast(width / 2f)) to
        y.coerceIn(half.coerceAtMost(height / 2f), (height - half).coerceAtLeast(height / 2f))
}

/** The clip-length pill turns ember once a recording is at least 1 s. */
fun clipLongEnough(elapsedMs: Long): Boolean = elapsedMs >= 1000L
