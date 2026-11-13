package app.sliver.feature.films

import app.sliver.data.FilmFormat
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

/** A film is always the whole project: every filled day, oldest first. */
fun buildFilmPlan(days: Collection<Day>): FilmPlan = FilmPlan(days.sortedBy { it.date })

fun Film.plan(days: Collection<Day>): FilmPlan = buildFilmPlan(days)

fun formatRuntime(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun LocalDate.short(): String = "$dayOfMonth ${MonthNames[monthNumber - 1]}"

/** "10 Jan – 12 Nov 2026" (years shown on both ends when they differ). */
fun FilmPlan.spanText(): String {
    val first = days.firstOrNull()?.date ?: return "No clips yet"
    val last = days.last().date
    return when {
        first == last -> "${first.short()} ${first.year}"
        first.year == last.year -> "${first.short()} – ${last.short()} ${last.year}"
        else -> "${first.short()} ${first.year} – ${last.short()} ${last.year}"
    }
}

fun clipsText(count: Int): String = if (count == 1) "1 clip" else "$count clips"

/** Poster, library and premiere subtitle: "10 Jan – 12 Nov 2026 · 129 clips". */
fun FilmPlan.subtitle(): String = if (days.isEmpty()) spanText() else "${spanText()} · ${clipsText(clips)}"

fun yearFilm(year: Int, format: FilmFormat, live: Boolean = true) = Film(
    id = if (live) DEFAULT_FILM_ID else "year-$year",
    name = "$year",
    format = format,
    live = live,
)

/** Creates the live film once, on the first saved clip of a project. */
suspend fun FilmRepository.ensureDefaultFilm(firstDay: LocalDate, format: FilmFormat) {
    if (observeAll().first().isEmpty()) save(yearFilm(firstDay.year, format))
}
