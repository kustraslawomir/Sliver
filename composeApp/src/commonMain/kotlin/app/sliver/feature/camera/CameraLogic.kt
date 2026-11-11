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

/** mm:ss, e.g. 00:07. */
fun formatTimer(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).coerceAtLeast(0)
    return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
}
