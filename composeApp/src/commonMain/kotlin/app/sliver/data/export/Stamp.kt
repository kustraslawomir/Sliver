package app.sliver.data.export

import kotlinx.datetime.LocalDate

private val ShortMonths = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

/** The one date stamp format, on screen and in exports: "2 NOV 2026". */
fun stampText(date: LocalDate): String = "${date.day} ${ShortMonths[date.month.ordinal]} ${date.year}"
