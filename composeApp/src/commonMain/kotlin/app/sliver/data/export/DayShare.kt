package app.sliver.data.export

import kotlinx.datetime.LocalDate

data class ShareOptions(val dateStamp: Boolean, val sound: Boolean, val mark: Boolean = false)

interface DayShare {
    /** Re-encodes the day clip at [source] with [options] into the cache and opens the system share sheet. */
    suspend fun share(source: String, date: LocalDate, options: ShareOptions)
}

fun stampText(date: LocalDate): String =
    "${date.day.toString().padStart(2, '0')}.${(date.month.ordinal + 1).toString().padStart(2, '0')}.${date.year}"
