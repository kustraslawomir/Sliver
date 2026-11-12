package app.sliver.feature.archive

import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.feature.camera.label
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class CellState { FILLED, BACKFILLED, TODAY, EMPTY, FUTURE }

/** Months shown in the grid: [first] and [last] bound navigation. */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth) = compareValuesBy(this, other, { it.year }, { it.month })

    fun plusMonths(n: Int): YearMonth {
        val index = year * 12 + (month - 1) + n
        return YearMonth(index / 12, index % 12 + 1)
    }
}

fun LocalDate.toYearMonth() = YearMonth(year, month.ordinal + 1)

fun canGoPrev(shown: YearMonth, first: LocalDate) = shown > first.toYearMonth()

fun canGoNext(shown: YearMonth, today: LocalDate) = shown < today.toYearMonth()

fun cellState(date: LocalDate, today: LocalDate, day: Day?, start: LocalDate? = null): CellState = when {
    date > today -> CellState.FUTURE
    day == null && start != null && date < start -> CellState.FUTURE
    day != null -> if (day.source == app.sliver.data.DaySource.BACKFILL) CellState.BACKFILLED else CellState.FILLED
    date == today -> CellState.TODAY
    else -> CellState.EMPTY
}

enum class MemoryKind(val label: String) {
    YEAR("One year ago"), HUNDRED("100 days ago"), MONTH("One month ago"),
}

data class Memory(val date: LocalDate, val kind: MemoryKind)

/** The most recent of 1 year / 100 days / 1 month ago that has a second. */
fun pickMemory(today: LocalDate, filled: Set<LocalDate>): Memory? = listOf(
    Memory(today.minus(DatePeriod(years = 1)), MemoryKind.YEAR),
    Memory(today.minus(DatePeriod(days = 100)), MemoryKind.HUNDRED),
    Memory(today.minus(DatePeriod(months = 1)), MemoryKind.MONTH),
).filter { it.date in filled }.maxByOrNull { it.date }

/** Year view: 12 rows of 31 entries; null where the day does not exist, 0 where no second, else ARGB. */
fun yearColors(year: Int, days: Map<LocalDate, Day>): List<List<Int?>> = List(12) { m ->
    val first = LocalDate(year, m + 1, 1)
    val length = first.daysUntil(first.plus(DatePeriod(months = 1)))
    List(31) { d -> if (d >= length) null else days[LocalDate(year, m + 1, d + 1)]?.color ?: 0 }
}

fun clipsHeader(count: Int, first: LocalDate, format: FilmFormat? = null): String =
    "$count ${if (count == 1) "clip" else "clips"} · since ${first.day} ${MonthNames[first.month.ordinal]}" +
        (format?.let { " · ${it.label()}" } ?: "")

val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
val MonthLong = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
