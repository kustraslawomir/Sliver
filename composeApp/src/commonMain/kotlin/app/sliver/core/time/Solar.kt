package app.sliver.core.time

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private fun rad(d: Double) = d * PI / 180
private fun deg(r: Double) = r * 180 / PI

/**
 * NOAA solar position: the instant on [date] (the local calendar day at [lon]) at which the sun crosses
 * [elevationDeg], rising or setting. Null when it never does that day (polar day or night).
 */
fun sunCrossing(date: LocalDate, lat: Double, lon: Double, elevationDeg: Double, rising: Boolean): Instant? {
    val days = date.toEpochDays().toLong()
    val jd = days + 2440587.5 + 0.5 - lon / 360
    val t = (jd - 2451545.0) / 36525.0
    val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
    val m = 357.52911 + t * (35999.05029 - 0.0001537 * t)
    val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val c = sin(rad(m)) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(rad(2 * m)) * (0.019993 - 0.000101 * t) + sin(rad(3 * m)) * 0.000289
    val omega = 125.04 - 1934.136 * t
    val lambda = l0 + c - 0.00569 - 0.00478 * sin(rad(omega))
    val eps0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
    val eps = eps0 + 0.00256 * cos(rad(omega))
    val decl = asin(sin(rad(eps)) * sin(rad(lambda)))
    val y = tan(rad(eps) / 2).let { it * it }
    val eqTime = 4 * deg(
        y * sin(2 * rad(l0)) - 2 * e * sin(rad(m)) + 4 * e * y * sin(rad(m)) * cos(2 * rad(l0)) -
            0.5 * y * y * sin(4 * rad(l0)) - 1.25 * e * e * sin(2 * rad(m)),
    )
    val cosH = (sin(rad(elevationDeg)) - sin(rad(lat)) * sin(decl)) / (cos(rad(lat)) * cos(decl))
    if (cosH < -1 || cosH > 1) return null
    val h = deg(acos(cosH))
    val noonMin = 720 - 4 * lon - eqTime
    val minutes = if (rising) noonMin - 4 * h else noonMin + 4 * h
    return Instant.fromEpochMilliseconds(((days * 1440 + minutes) * 60_000).toLong())
}

const val SUNSET_ELEVATION = -0.833

/** Golden hour: the sun between -4 and +6 degrees. */
const val GOLDEN_LOW = -4.0
const val GOLDEN_HIGH = 6.0

data class GoldenWindow(val start: Instant, val end: Instant)

/** Morning and evening golden windows of [date], in time order. */
fun goldenWindows(date: LocalDate, lat: Double, lon: Double): List<GoldenWindow> = listOfNotNull(
    window(sunCrossing(date, lat, lon, GOLDEN_LOW, true), sunCrossing(date, lat, lon, GOLDEN_HIGH, true)),
    window(sunCrossing(date, lat, lon, GOLDEN_HIGH, false), sunCrossing(date, lat, lon, GOLDEN_LOW, false)),
)

private fun window(a: Instant?, b: Instant?) = if (a != null && b != null) GoldenWindow(a, b) else null

sealed interface GoldenState {
    data class Soon(val minutes: Int) : GoldenState
    data class Now(val minutesLeft: Int) : GoldenState
}

const val GOLDEN_LOOKAHEAD_MINUTES = 180

/** The running golden hour, or the next one if it starts within three hours; null otherwise. */
fun goldenState(now: Instant, today: LocalDate, lat: Double, lon: Double): GoldenState? {
    val windows = listOf(today.toEpochDays() - 1, today.toEpochDays(), today.toEpochDays() + 1)
        .flatMap { goldenWindows(LocalDate.fromEpochDays(it), lat, lon) }.sortedBy { it.start }
    windows.firstOrNull { now >= it.start && now < it.end }?.let {
        return GoldenState.Now(((it.end - now).inWholeSeconds / 60 + 1).toInt())
    }
    val next = windows.firstOrNull { it.start > now } ?: return null
    val mins = ((next.start - now).inWholeSeconds + 59).toInt() / 60
    return if (mins <= GOLDEN_LOOKAHEAD_MINUTES) GoldenState.Soon(mins) else null
}
