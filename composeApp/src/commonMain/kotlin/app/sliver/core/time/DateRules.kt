package app.sliver.core.time

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun Instant.toLocalDate(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    toLocalDateTime(zone).date

fun todayLocal(clock: Clock = AppClock, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    clock.now().toLocalDate(zone)

fun LocalDate.startOfDay(zone: TimeZone = TimeZone.currentSystemDefault()): Instant = atStartOfDayIn(zone)

/** Monday-first week containing [date], 7 dates. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
    return List(7) { monday.plus(DatePeriod(days = it)) }
}

/** Monday-first month grid: 6 rows x 7 cells; null = blank cell (leading or trailing). */
fun monthGrid(year: Int, month: Int): List<List<LocalDate?>> {
    val first = LocalDate(year, month, 1)
    val lead = first.dayOfWeek.isoDayNumber - 1
    val length = first.daysUntil(first.plus(DatePeriod(months = 1)))
    val cells: List<LocalDate?> = List(42) { i ->
        val day = i - lead + 1
        if (day in 1..length) LocalDate(year, month, day) else null
    }
    return cells.chunked(7)
}
