package app.sliver.feature.films

import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.repo.FilmRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

const val DEFAULT_FILM_ID = "default-year"

/** Day files of a film in date order; open days (no clip) are skipped. */
data class FilmPlan(val days: List<Day>) {
    val clips: Int get() = days.size
    val files: List<String> get() = days.map { it.file }
}

fun buildFilmPlan(days: Collection<Day>, from: LocalDate?, to: LocalDate?): FilmPlan =
    FilmPlan(days.filter { (from == null || it.date >= from) && (to == null || it.date <= to) }.sortedBy { it.date })

fun Film.plan(days: Collection<Day>): FilmPlan = buildFilmPlan(days, rangeFrom, rangeTo)

fun formatRuntime(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun LocalDate.short(): String = "${MonthNames[monthNumber - 1]} $dayOfMonth"

fun Film.rangeText(): String {
    val from = rangeFrom ?: return "All days"
    val to = rangeTo ?: return "From ${from.short()}"
    return when {
        rangeType == FilmRangeType.YEAR && from.monthNumber == 1 && from.dayOfMonth == 1 -> "${from.year}"
        from.year == to.year -> "${from.short()} – ${to.short()}, ${to.year}"
        else -> "${from.short()}, ${from.year} – ${to.short()}, ${to.year}"
    }
}

fun yearFilm(year: Int, format: FilmFormat, live: Boolean = true) = Film(
    id = if (live) DEFAULT_FILM_ID else "year-$year",
    name = "$year",
    format = format,
    rangeType = FilmRangeType.YEAR,
    rangeFrom = LocalDate(year, 1, 1),
    rangeTo = LocalDate(year, 12, 31),
    live = live,
)

/** Creates the live '<year>' film once, on the first saved second. */
suspend fun FilmRepository.ensureDefaultFilm(firstDay: LocalDate, format: FilmFormat) {
    if (observeAll().first().isEmpty()) save(yearFilm(firstDay.year, format))
}

enum class TeaserState { HIDDEN, LOCKED, UNLOCKED }

/** Year in Seconds teaser: visible from 1 Dec, unlocked from 26 Dec (local date). */
fun teaserState(today: LocalDate): TeaserState = when {
    today.monthNumber == 12 && today.dayOfMonth >= 26 -> TeaserState.UNLOCKED
    today.monthNumber == 12 -> TeaserState.LOCKED
    else -> TeaserState.HIDDEN
}
