package app.sliver.feature.gallery

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class GalleryItem(
    val uri: String,
    val isVideo: Boolean,
    val date: LocalDate,
    val durationMs: Long,
)

/** EXIF 'yyyy:MM:dd HH:mm:ss' (camera local time). */
fun parseExifDate(raw: String?): LocalDate? {
    val m = raw?.let { Regex("""^(\d{4}):(\d{2}):(\d{2})[ T]""").find(it.trim()) } ?: return null
    val (y, mo, d) = m.destructured
    return runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
}

/** MediaMetadataRetriever date, 'yyyyMMdd'T'HHmmss.SSS'Z'' in UTC, shown in [zone]. */
fun parseVideoDate(raw: String?, zone: TimeZone): LocalDate? {
    val m = raw?.let { Regex("""^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})""").find(it.trim()) } ?: return null
    val (y, mo, d, h, mi, s) = m.destructured
    return runCatching {
        val utc = LocalDateTime(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), mi.toInt(), s.toInt())
        val instant = Instant.fromEpochSeconds(utc.toInstantUtcSeconds())
        instant.toLocalDateTime(zone).date
    }.getOrNull()
}

private fun LocalDateTime.toInstantUtcSeconds(): Long =
    date.toEpochDays() * 86_400L + hour * 3_600L + minute * 60L + second
