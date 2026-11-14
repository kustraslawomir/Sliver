package app.sliver.feature.gallery

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** How well we know when a picked item was recorded. */
sealed interface CaptureDate {
    /** EXIF DateTimeOriginal, QuickTime creation date or a camera's MediaStore DATE_TAKEN. */
    data class Known(val date: LocalDate) : CaptureDate

    /** Date found in the file name: when it was sent or saved, not proof of when it was recorded. */
    data class Hint(val date: LocalDate) : CaptureDate

    data object Unknown : CaptureDate
}

data class GalleryItem(
    val uri: String,
    val isVideo: Boolean,
    val captured: CaptureDate,
    val durationMs: Long,
    val name: String? = null,
    val takenAtMs: Long? = null,
)

/** A picked item and, for a Live Photo, the short video that belongs to it. */
data class MediaGroup(val primary: GalleryItem, val liveVideo: GalleryItem? = null)

private const val LIVE_PAIR_WINDOW_MS = 2_000L
private const val LIVE_MAX_VIDEO_MS = 6_000L

private fun baseName(name: String?): String? = name?.substringBeforeLast('.')?.lowercase()?.takeIf { it.isNotEmpty() }

/**
 * Joins a still with a short video into one Live Photo when they share a base name (IMG_1234.HEIC + IMG_1234.MOV) or were
 * captured within 2 s of each other. Anything else stays a group of one.
 */
fun groupLivePhotos(items: List<GalleryItem>): List<MediaGroup> {
    val videos = items.filter { it.isVideo && it.durationMs <= LIVE_MAX_VIDEO_MS }.toMutableList()
    val used = mutableSetOf<String>()
    val groups = items.filter { !it.isVideo }.map { still ->
        val base = baseName(still.name)
        val match = videos.firstOrNull { v ->
            (base != null && base == baseName(v.name)) ||
                (still.takenAtMs != null && v.takenAtMs != null && kotlin.math.abs(still.takenAtMs - v.takenAtMs) <= LIVE_PAIR_WINDOW_MS)
        }
        if (match != null) {
            videos.remove(match)
            used += match.uri
        }
        MediaGroup(still, match)
    }
    return groups + items.filter { it.isVideo && it.uri !in used }.map { MediaGroup(it) }
}

private val nameDate = Regex("""(?<!\d)(20\d{2})[-_]?(\d{2})[-_]?(\d{2})(?!\d)""")

/**
 * Date in a messenger or camera file name: Telegram `photo_2026-11-10_14-22-31`, WhatsApp `IMG-20261110-WA0001`,
 * Signal `signal-2026-11-10-142231`, `IMG_20261110_142231`, `PXL_20261110_142231`, `Screenshot_20261110-142231`.
 */
fun filenameDateHint(name: String?): LocalDate? {
    val m = name?.let { nameDate.find(it) } ?: return null
    val (y, mo, d) = m.destructured
    return runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
}

/** [camera] = EXIF / QuickTime / camera DATE_TAKEN. MediaStore DATE_MODIFIED and DATE_ADDED are never passed in: messengers set them to the day received. */
fun resolveCaptureDate(camera: LocalDate?, name: String?): CaptureDate =
    camera?.let { CaptureDate.Known(it) } ?: filenameDateHint(name)?.let { CaptureDate.Hint(it) } ?: CaptureDate.Unknown

private val exifPattern = Regex("""^(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})""")
private val exifDateOnly = Regex("""^(\d{4}):(\d{2}):(\d{2})[ T]""")
private val videoPattern = Regex("""^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})""")

private fun plausible(d: LocalDate): LocalDate? = d.takeIf { it.year >= 2000 }

/** EXIF 'yyyy:MM:dd HH:mm:ss' (camera local time). */
fun parseExifDate(raw: String?): LocalDate? {
    val m = raw?.let { exifDateOnly.find(it.trim()) } ?: return null
    val (y, mo, d) = m.destructured
    return runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()?.let(::plausible)
}

/** EXIF time as epoch millis, reading the camera's local clock in [zone]. */
fun parseExifMillis(raw: String?, zone: TimeZone): Long? {
    val m = raw?.let { exifPattern.find(it.trim()) } ?: return null
    val (y, mo, d, h, mi, s) = m.destructured
    return runCatching {
        LocalDateTime(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), mi.toInt(), s.toInt()).toInstant(zone).toEpochMilliseconds()
    }
        .getOrNull()
}

/** MediaMetadataRetriever date, 'yyyyMMdd'T'HHmmss.SSS'Z'' in UTC. QuickTime/MP4 creation time, so also iPhone .mov. */
fun parseVideoMillis(raw: String?): Long? {
    val m = raw?.let { videoPattern.find(it.trim()) } ?: return null
    val (y, mo, d, h, mi, s) = m.destructured
    return runCatching {
        val utc = LocalDateTime(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), mi.toInt(), s.toInt())
        (utc.date.toEpochDays() * 86_400L + h.toInt() * 3_600L + mi.toInt() * 60L + s.toInt()) * 1000
    }.getOrNull()
}

/** The same as [parseVideoMillis], shown as a calendar day in [zone]. */
fun parseVideoDate(raw: String?, zone: TimeZone): LocalDate? =
    parseVideoMillis(raw)?.let { plausible(Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date) }

sealed interface AddCheck {
    data object Allowed : AddCheck
    data class AllowedWithNotice(val message: String) : AddCheck
    data class Blocked(val message: String) : AddCheck
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatDay(d: LocalDate) = "${d.day} ${monthNames[d.month.ordinal]} ${d.year}"

// DECISION: a known capture date pins the item to that day. A date from the file name, or no date at all, never blocks
// (messengers strip EXIF): the item goes to the day the user chose, with a one-line notice.
fun checkCaptureDate(captured: CaptureDate, target: LocalDate): AddCheck = when (captured) {
    is CaptureDate.Known ->
        if (captured.date == target) {
            AddCheck.Allowed
        } else {
            AddCheck.Blocked("This clip was recorded on ${formatDay(captured.date)}. It can only be added to that day.")
        }
    is CaptureDate.Hint -> AddCheck.AllowedWithNotice("Sent on ${formatDay(captured.date)} — added to ${formatDay(target)}")
    CaptureDate.Unknown -> AddCheck.AllowedWithNotice("Date unknown — added to ${formatDay(target)}")
}

fun isHeif(mime: String?): Boolean = mime == "image/heic" || mime == "image/heif"

/** One tile on the day picker: the item and, for a Live/motion pair, the short video that belongs to it. */
data class DayItem(val entry: app.sliver.feature.autoimport.MediaEntry, val liveVideo: String? = null) {
    val live: Boolean get() = liveVideo != null || entry.motion || entry.live
}

/**
 * Audit v1.8 §6: the day picker's sections. Full recordings the app saved to Photos (named "<app>_…") come first as
 * "Recorded in Reelog"; the rest drops exact duplicates (same name and size) and shows a still + its short video as one
 * Live tile.
 */
fun dayPickerSections(list: List<app.sliver.feature.autoimport.MediaEntry>, appName: String): Pair<List<DayItem>, List<DayItem>> {
    val (reelog, rest) = list.partition { it.video && it.name?.startsWith("${appName}_") == true }
    val unique = rest.distinctBy { e -> if (e.name != null && e.sizeBytes > 0) "${e.name}|${e.sizeBytes}" else e.uri }
    val used = mutableSetOf<String>()
    val items = mutableListOf<DayItem>()
    for (e in unique.sortedBy { it.video }) {
        if (e.uri in used) continue
        if (!e.video) {
            val pair = unique.firstOrNull { v ->
                v.video && v.uri !in used && v.durationMs in 1..LIVE_MAX_VIDEO_MS &&
                    (baseName(v.name) != null && baseName(v.name) == baseName(e.name) || kotlin.math.abs(v.takenAtMs - e.takenAtMs) <= LIVE_PAIR_WINDOW_MS)
            }
            pair?.let { used += it.uri }
            items += DayItem(e, pair?.uri)
        } else {
            items += DayItem(e)
        }
        used += e.uri
    }
    val order = list.map { it.uri }
    return reelog.sortedByDescending { it.takenAtMs }.map { DayItem(it) } to items.sortedBy { order.indexOf(it.entry.uri) }
}
