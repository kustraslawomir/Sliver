package app.sliver.data.export

/** Stamp geometry is authored for a 360 dp wide frame (12 sp text, 16 dp margin) and scaled to the frame's short side. */
const val STAMP_REFERENCE_SIDE = 360f
const val STAMP_TEXT_SP = 12f
const val STAMP_MARGIN_DP = 16f

fun stampScale(frameW: Int, frameH: Int): Float = minOf(frameW, frameH) / STAMP_REFERENCE_SIDE

fun stampTextPx(frameW: Int, frameH: Int): Int = (STAMP_TEXT_SP * stampScale(frameW, frameH)).toInt()

/** Normalised frame coordinates (-1..1, y up) of the bottom-right corner inset by the scaled margin. */
fun stampAnchor(frameW: Int, frameH: Int): Pair<Float, Float> {
    val margin = STAMP_MARGIN_DP * stampScale(frameW, frameH)
    return (1f - 2f * margin / frameW) to (-1f + 2f * margin / frameH)
}

/** LED stamp digit height at the 360 dp reference side, and its colour (film content, not UI chrome). */
const val LED_DIGIT_DP = 16f
const val LED_ORANGE = 0xFFFF9A3CL
