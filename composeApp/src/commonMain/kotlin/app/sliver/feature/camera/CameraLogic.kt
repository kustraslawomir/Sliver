package app.sliver.feature.camera

import app.sliver.data.FilmFormat

const val HOLD_THRESHOLD_MS = 300L

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

const val STORY_ASPECT = 9f / 16f

/** Width of the 9:16 band as a fraction of a landscape frame of [width] x [height]. */
fun storyBandFraction(width: Float, height: Float): Float =
    if (width <= 0f || height <= 0f) 1f else (height * STORY_ASPECT / width).coerceAtMost(1f)

/** 0..1 crop offset (0 = band at left) for a band of [bandFraction] whose left edge sits at [leftFraction]. */
fun cropXFromLeft(leftFraction: Float, bandFraction: Float): Float = cropYFromTop(leftFraction, bandFraction)

fun leftFromCropX(cropX: Float, bandFraction: Float): Float = topFromCropY(cropX, bandFraction)

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
const val FOCUS_RING_START_DP = 84
const val FOCUS_RING_END_DP = 70
const val FOCUS_LOCK_PILL_OFFSET_DP = 58

/** Tap position inside the viewfinder, clamped to its bounds, as 0..1 fractions. */
fun focusFraction(x: Float, y: Float, width: Float, height: Float): Pair<Float, Float> =
    if (width <= 0f || height <= 0f) {
        0.5f to 0.5f
    } else {
        (x / width).coerceIn(0f, 1f) to (y / height).coerceIn(0f, 1f)
    }

/** Ring is not drawn past the viewfinder edge: keeps the centre of a ring of [ringDp] fully visible. */
fun focusRingCenter(x: Float, y: Float, width: Float, height: Float, ringPx: Float): Pair<Float, Float> {
    val half = ringPx / 2f
    return x.coerceIn(half.coerceAtMost(width / 2f), (width - half).coerceAtLeast(width / 2f)) to
        y.coerceIn(half.coerceAtMost(height / 2f), (height - half).coerceAtLeast(height / 2f))
}

/** The clip-length pill turns red once a recording is at least 1 s. */
fun clipLongEnough(elapsedMs: Long): Boolean = elapsedMs >= 1000L

const val EXPOSURE_HIDE_AFTER_MS = 3000L
const val EXPOSURE_TRACK_DP = 120

/** Exposure index for a thumb at [fraction] of the track (0 = top = brightest, 1 = bottom = darkest). */
fun exposureIndexAt(fraction: Float, min: Int, max: Int): Int =
    (max - fraction.coerceIn(0f, 1f) * (max - min)).let { kotlin.math.round(it).toInt() }.coerceIn(min, max)

/** Inverse of [exposureIndexAt]: thumb position for an index. */
fun exposureFraction(index: Int, min: Int, max: Int): Float =
    if (max <= min) 0.5f else ((max - index).toFloat() / (max - min)).coerceIn(0f, 1f)

/** Signed EV text, e.g. "+0.7", "-1.3", "0.0". */
fun formatEv(index: Int, step: Float): String {
    val tenths = kotlin.math.round(index * step * 10f).toInt()
    val sign = if (tenths > 0) "+" else if (tenths < 0) "-" else ""
    val a = kotlin.math.abs(tenths)
    return "$sign${a / 10}.${a % 10}"
}

/** The slider sits on the side of the focus point with more room. */
fun exposureOnRight(focusX: Float, width: Float): Boolean = focusX < width / 2f

/** Zoom stops like the system camera: the ultra-wide (when the lens goes below 1×), 1×, 2× and 3× within [min]..[max]. */
fun zoomPresets(min: Float, max: Float): List<Float> = buildList {
    if (min < 0.95f) add(kotlin.math.round(min * 10f) / 10f)
    add(1f)
    if (max >= 2f) add(2f)
    if (max >= 3f) add(3f)
}

/** Index of the stop the current [zoom] belongs to: the highest stop not above it. */
fun activeZoomPreset(presets: List<Float>, zoom: Float): Int =
    presets.indexOfLast { it <= zoom + 0.01f }.coerceAtLeast(0)

/** "0.6", "2", or with [unit] "1.4×". */
fun formatZoom(zoom: Float, unit: Boolean): String {
    val tenths = kotlin.math.round(zoom * 10f).toInt()
    val text = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    return if (unit) "$text×" else text
}
