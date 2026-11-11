package app.sliver.feature.archive

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Nearest filled days around [date], skipping gaps. */
fun prevDay(date: LocalDate, filled: Collection<LocalDate>): LocalDate? = filled.filter { it < date }.maxOrNull()

fun nextDay(date: LocalDate, filled: Collection<LocalDate>): LocalDate? = filled.filter { it > date }.minOrNull()

fun dayNumber(date: LocalDate, first: LocalDate): Int = first.daysUntil(date) + 1

fun weekdayName(date: LocalDate): String = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
