package app.sliver.data.export

import app.sliver.data.StampStyle
import kotlinx.datetime.LocalDate

private val ShortMonths = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

/** The default date stamp format, on screen and in exports: "2 NOV 2026". */
fun stampText(date: LocalDate): String = "${date.day} ${ShortMonths[date.month.ordinal]} ${date.year}"

/** Stamp text for [style], always day first (owner: European order): LED "02 11 '26", camcorder "2 NOV. 2026". */
fun stampText(date: LocalDate, style: StampStyle): String = when (style) {
    StampStyle.PLAIN -> stampText(date)
    StampStyle.LED -> "${date.day.toString().padStart(2, '0')} ${date.monthNumber.toString().padStart(2, '0')} '${(date.year % 100).toString().padStart(2, '0')}"
    StampStyle.CAMCORDER -> "${date.day} ${ShortMonths[date.month.ordinal]}. ${date.year}"
}

/** A filled rectangle in a segmented glyph row, in the row's own units (top-left origin). */
class SegRect(val x: Float, val y: Float, val w: Float, val h: Float)

/** Seven-segment digits for the LED stamp: digits, spaces and an apostrophe; any other character is skipped. */
object SevenSegment {
    // a b c d e f g
    private val Digits = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abdeg", '3' to "abcdg", '4' to "bcfg",
        '5' to "acdfg", '6' to "acdefg", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg",
    )
    private const val WIDTH = 0.55f
    private const val THICK = 0.13f
    private const val GAP = 0.25f
    private const val SPACE = 0.55f

    fun width(text: String, height: Float): Float = layout(text, height).second

    fun rects(text: String, height: Float): List<SegRect> = layout(text, height).first

    private fun layout(text: String, h: Float): Pair<List<SegRect>, Float> {
        val out = ArrayList<SegRect>()
        var x = 0f
        val w = WIDTH * h
        val t = THICK * h
        for (ch in text) {
            when {
                ch == ' ' -> x += SPACE * h
                ch == '\'' -> { out += SegRect(x, 0f, t, 0.3f * h); x += t + GAP * h }
                Digits.containsKey(ch) -> {
                    val on = Digits.getValue(ch)
                    if ('a' in on) out += SegRect(x + t / 2, 0f, w - t, t)
                    if ('b' in on) out += SegRect(x + w - t, t / 2, t, h / 2 - t)
                    if ('c' in on) out += SegRect(x + w - t, h / 2 + t / 2, t, h / 2 - t)
                    if ('d' in on) out += SegRect(x + t / 2, h - t, w - t, t)
                    if ('e' in on) out += SegRect(x, h / 2 + t / 2, t, h / 2 - t)
                    if ('f' in on) out += SegRect(x, t / 2, t, h / 2 - t)
                    if ('g' in on) out += SegRect(x + t / 2, h / 2 - t / 2, w - t, t)
                    x += w + GAP * h
                }
            }
        }
        return out to maxOf(0f, x - GAP * h)
    }
}
