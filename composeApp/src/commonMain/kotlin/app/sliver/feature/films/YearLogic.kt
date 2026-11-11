package app.sliver.feature.films

import app.sliver.data.FilmRangeType
import app.sliver.data.LookPreset
import app.sliver.data.db.Day
import app.sliver.data.db.Film

data class YearStats(
    val seconds: Int,
    val films: Int,
    val topMonths: List<Int>,
    val favouriteLook: LookPreset?,
)

fun yearStats(year: Int, days: Collection<Day>, films: Collection<Film>): YearStats {
    val mine = days.filter { it.date.year == year }
    val perMonth = mine.groupingBy { it.date.monthNumber }.eachCount()
    val maxMonth = perMonth.values.maxOrNull()
    val looks = mine.groupingBy { it.look.preset }.eachCount()
    val maxLook = looks.values.maxOrNull()
    return YearStats(
        seconds = mine.size,
        films = films.count { it.renderPath != null },
        topMonths = perMonth.filterValues { it == maxMonth }.keys.sorted(),
        favouriteLook = looks.entries.filter { it.value == maxLook }.minByOrNull { it.key.ordinal }?.key,
    )
}

private val FullMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

fun monthsText(months: List<Int>): String =
    if (months.isEmpty()) "None yet" else months.joinToString(" · ") { FullMonths[it - 1] }

fun lookName(look: LookPreset): String = when (look) {
    LookPreset.VHS_94 -> "VHS 94"
    LookPreset.NIGHT_NEON -> "Night Neon"
    LookPreset.COLD_STEEL -> "Cold Steel"
    LookPreset.HARD_LIGHT -> "Hard Light"
    else -> look.name.lowercase().replaceFirstChar { it.uppercase() }
}

/** The live film or an existing year film for that year; null means the caller creates one. */
fun findYearFilm(year: Int, films: Collection<Film>): Film? =
    films.firstOrNull { it.rangeType == FilmRangeType.YEAR && it.rangeFrom?.year == year }

/** A render is stale when missing or older than any day it contains. */
fun Film.isStale(days: Collection<Day>): Boolean {
    if (renderPath == null) return true
    val at = renderedAt ?: return true
    return plan(days).days.any { it.createdAt > at }
}
