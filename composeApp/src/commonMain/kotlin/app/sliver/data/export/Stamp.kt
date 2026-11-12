package app.sliver.data.export

import kotlinx.datetime.LocalDate

fun stampText(date: LocalDate): String =
    "${date.day.toString().padStart(2, '0')}.${(date.month.ordinal + 1).toString().padStart(2, '0')}.${date.year}"
