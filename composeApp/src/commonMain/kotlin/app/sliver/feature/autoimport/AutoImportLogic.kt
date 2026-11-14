package app.sliver.feature.autoimport

import app.sliver.data.db.Day
import app.sliver.feature.archive.MonthLong
import app.sliver.feature.archive.weekdayName
import app.sliver.feature.gallery.formatDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** One photo, video or motion photo from the gallery with a real capture time. */
/** [camera] = taken with the phone's camera (DCIM/Camera…), not saved, received or downloaded. Screenshots never get here. */
data class MediaEntry(
    val uri: String,
    val video: Boolean,
    val takenAtMs: Long,
    val durationMs: Long = 0,
    val motion: Boolean = false,
    val camera: Boolean = true,
    /** Folder items only: file name and size (duplicates), and whether the date is just the file's modified time. */
    val name: String? = null,
    val sizeBytes: Long = 0,
    val fileTime: Boolean = false,
    /** The short video half of an iOS Live Photo (the still is dropped); ranks with motion photos. */
    val live: Boolean = false,
)

/** Screenshots and screen recordings are never a day's moment. */
fun isScreenCapture(relativePath: String?, name: String?): Boolean {
    val s = "${relativePath.orEmpty()}/${name.orEmpty()}".lowercase()
    return "screenshot" in s || "screen record" in s || "screenrecord" in s || "screen_record" in s || "screencast" in s
}

/** The app's own demo clips (Movies/SliverDemo, Pictures/SliverDemo) are test media, never a day's moment. */
fun isDemoMedia(relativePath: String?): Boolean = "sliverdemo" in relativePath.orEmpty().lowercase()

/** File name inside a document (folder) uri, e.g. ".../document/primary%3ALena%2F20260403_211310.jpg" → "20260403_211310.jpg". */
fun documentName(uri: String): String? {
    if ("/document/" !in uri) return null
    val doc = uri.substringAfterLast("/document/").replace("%2F", "/").replace("%2f", "/").replace("%3A", ":").replace("%3a", ":")
    return doc.substringAfterLast('/').substringAfterLast(':').replace("%20", " ").takeIf { it.isNotEmpty() }
}

/** How a rejected item is remembered: its uri and, when known, its file name. */
fun dismissTokens(c: DraftCandidate): List<String> = listOfNotNull(c.uri, (c.name ?: documentName(c.uri))?.let {
    "name:$it"
})

/**
 * Owner 13 Nov: an item rejected while importing from a folder came back when importing from the gallery (same file,
 * different uri). Matches by uri or by file name.
 */
fun isDismissed(uri: String, name: String?, seen: Collection<String>): Boolean {
    if (uri in seen) return true
    val n = name ?: documentName(uri) ?: return false
    return "name:$n" in seen || seen.any { documentName(it) == n }
}

/** Camera originals live under DCIM (e.g. DCIM/Camera/, DCIM/100ANDRO/); messenger and download folders don't. */
fun isCameraPath(relativePath: String?): Boolean = relativePath.orEmpty().lowercase().startsWith("dcim/")

/** A gallery item offered for a draft day; stored as JSON on the day. */
@Serializable
data class DraftCandidate(
    val uri: String,
    val video: Boolean,
    val durationMs: Long = 0,
    val motion: Boolean = false,
    val fileTime: Boolean = false,
    val live: Boolean = false,
    /** File name, so a rejected item is recognised whichever way it was found (gallery or folder). */
    val name: String? = null,
)

const val MAX_CANDIDATES = 30

// DECISION: a motion photo carries no readable duration up front; it ranks like a video of this nominal length.
const val MOTION_NOMINAL_MS = 1_500L

fun localDateOf(ms: Long, zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).date

/** Entries grouped by local capture date inside [from]..[to]; entries without a capture time (0) are ignored. */
fun groupByDate(entries: List<MediaEntry>, zone: TimeZone, from: LocalDate, to: LocalDate): Map<LocalDate, List<MediaEntry>> =
    entries.filter { it.takenAtMs > 0 }
        .groupBy { localDateOf(it.takenAtMs, zone) }
        .filterKeys { it in from..to }

/** Days in [from]..[to] that have no row (filled or draft). */
fun gapDays(from: LocalDate, to: LocalDate, hasRow: (LocalDate) -> Boolean): List<LocalDate> = datesBetween(from, to).filterNot(hasRow)

fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> =
    if (from > to) emptyList() else (from.toEpochDays()..to.toEpochDays()).map(LocalDate::fromEpochDays)

/** Default first: longest video (a motion photo counts as a video), then photos; ties by capture time. */
fun rank(entries: List<MediaEntry>): List<MediaEntry> =
    // Owner: videos first (longest first), then moving photos (Motion Photo, iOS Live Photo), then stills (JPEG, HEIC…);
    // camera originals before saved / received media inside each group.
    entries.sortedWith(
        compareBy<MediaEntry> { mediaTier(it) }.thenBy { !it.camera }
            .thenByDescending { if (mediaTier(it) == 0) it.durationMs else 0L }
            .thenBy { it.takenAtMs }.thenBy { it.uri },
    )

/** 0 video, 1 moving photo, 2 still. */
fun mediaTier(e: MediaEntry): Int = when {
    e.motion || e.live -> 1
    e.video -> 0
    else -> 2
}

fun candidatesOf(entries: List<MediaEntry>): List<DraftCandidate> =
    rank(entries).take(MAX_CANDIDATES).map {
        DraftCandidate(it.uri, it.video, it.durationMs, it.motion, it.fileTime, it.live, it.name)
    }

/** A video's 1 s window sits in the middle. */
fun middleWindowStartMs(durationMs: Long): Long = ((durationMs - 1000) / 2).coerceAtLeast(0)

private val json = Json { ignoreUnknownKeys = true }

fun encodeCandidates(list: List<DraftCandidate>): String = json.encodeToString(ListSerializer(DraftCandidate.serializer()), list)

fun decodeCandidates(raw: String?): List<DraftCandidate> =
    raw?.let {
        runCatching { json.decodeFromString(ListSerializer(DraftCandidate.serializer()), it) }.getOrNull()
    }.orEmpty()

@Serializable
data class ImportRun(
    val id: String,
    val projectId: String,
    val from: String,
    val done: Int = 0,
    val total: Int = 0,
    val finished: Boolean = false,
    /** Owner: "it should be automated" — true saves normal days right away instead of drafts. */
    val keep: Boolean = false,
    /** Progress stats for the run screen (all optional so older stored runs still decode). */
    val gapDays: Int = 0,
    val scanned: Int = -1,
    val videos: Int = 0,
    val photos: Int = 0,
    val skipped: Int = 0,
    val current: String? = null,
    val startedAt: Long = 0,
    /** First and last day that actually got a clip, to show where the gallery had something. */
    val firstFilled: String? = null,
    /** Default: only camera photos and videos; saved/received media only when the user asks for them. */
    val cameraOnly: Boolean = true,
    /** Owner 13 Nov: items rejected before may be offered again, only when the user asks (default off). */
    val includeRejected: Boolean = false,
    val lastFilled: String? = null,
    /** Folder source: tree uri to scan (null = gallery), plus what the scan found. */
    val folder: String? = null,
    val folderName: String? = null,
    val found: Int = 0,
    val unreadable: Int = 0,
    val fileTimeDates: Int = 0,
    val truncated: Boolean = false,
) {
    val progress get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val scanning get() = scanned < 0
}

/** "About 2 min left" from the pace so far; null until a few days are done. */
fun etaLabel(done: Int, total: Int, startedAt: Long, now: Long, alreadyDone: Int = 0): String? {
    val made = done - alreadyDone
    if (made < 3 || total <= done || startedAt <= 0) return null
    val perDay = (now - startedAt).toDouble() / made
    val secs = (perDay * (total - done) / 1000).toLong()
    return when {
        secs < 60 -> "Less than a minute left"
        secs < 3600 -> "About ${(secs + 59) / 60} min left"
        else -> "About ${secs / 3600} h ${(secs % 3600) / 60} min left"
    }
}

fun groupThousands(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

fun emptyDaysLabel(count: Int, from: LocalDate) = "$count empty ${if (count == 1) "day" else "days"} since ${formatDay(from)}"

/** Big count on the Auto-import page. */
fun fillLabel(count: Int) = if (count == 0) "No empty days to fill" else "$count empty ${if (count == 1) "day" else "days"} to fill"

enum class FromPreset { PROJECT_START, THIS_YEAR, LAST_30, CUSTOM }

/** Quick "from" dates; a preset equal to an earlier one is dropped so the chips never repeat a date. */
fun fromPresets(projectStart: LocalDate, today: LocalDate): List<Pair<FromPreset, LocalDate>> {
    val list = listOf(
        FromPreset.PROJECT_START to projectStart,
        FromPreset.THIS_YEAR to LocalDate(today.year, 1, 1),
        FromPreset.LAST_30 to LocalDate.fromEpochDays(today.toEpochDays() - 30),
    )
    return list.filter { (_, d) -> d <= today }.distinctBy { it.second }
}

fun presetLabel(p: FromPreset) = when (p) {
    FromPreset.PROJECT_START -> "Project start"
    FromPreset.THIS_YEAR -> "This year"
    FromPreset.LAST_30 -> "Last 30 days"
    FromPreset.CUSTOM -> "Pick date…"
}

fun draftsChipLabel(count: Int) = "$count ${if (count == 1) "draft" else "drafts"} to review"

fun draftPositionLabel(index: Int, total: Int) = "Draft · ${index + 1} of $total"

/** Editor header: auto-filled days (already in the project) vs drafts. */
fun draftHeaderTitle(draft: Boolean, index: Int, total: Int) = "${if (draft) "Draft" else "Auto-filled"} · ${index + 1} of $total"

fun importSummary(done: Int, failed: Int, keep: Boolean = false): String =
    "$done ${if (keep) (if (done == 1) "day filled" else "days filled") else (if (done == 1) "draft" else "drafts") + " added"}" +
        if (failed > 0) ", $failed couldn't be read" else ""

/** Index of the draft after [date] in [drafts] (sorted by date), wrapping; null when no other draft remains. */
fun <T> nextDraft(drafts: List<T>, date: LocalDate, dateOf: (T) -> LocalDate): T? =
    drafts.firstOrNull { dateOf(it) > date } ?: drafts.firstOrNull { dateOf(it) < date }

fun <T> previousDraft(drafts: List<T>, date: LocalDate, dateOf: (T) -> LocalDate): T? =
    drafts.lastOrNull { dateOf(it) < date } ?: drafts.lastOrNull { dateOf(it) > date }

/** A day saved by a "keep without review" run that the user has not looked at yet. */
fun Day.isAuto(): Boolean = !draft && importRunId != null

fun autoDays(all: List<Day>): List<Day> = all.filter { it.isAuto() }

/** Everything waiting for a look: drafts and auto-filled days, by date. */
fun toCheck(drafts: List<Day>, all: List<Day>): List<Day> = (drafts + autoDays(all)).sortedBy { it.date }

private fun daysWord(n: Int) = if (n == 1) "day" else "days"

fun reviewChipLabel(drafts: Int, auto: Int): String = when {
    auto == 0 -> draftsChipLabel(drafts)
    drafts == 0 -> "$auto auto-filled ${daysWord(auto)} to check"
    else -> "${drafts + auto} days to check"
}

fun reviewSheetTitle(drafts: Int, auto: Int): String = when {
    auto == 0 -> "$drafts ${if (drafts == 1) "draft" else "drafts"}"
    drafts == 0 -> "$auto auto-filled ${daysWord(auto)}"
    else -> "${drafts + auto} days to check"
}

fun reviewSheetBody(drafts: Int, auto: Int): String = when {
    drafts == 0 -> "Picked from your camera roll and already in the project. Check them, or keep them as they are."
    auto == 0 -> "Picked from your camera roll. They don't count until you keep them. Check them, or keep them as they are."
    else -> "Picked from your camera roll. Drafts don't count until you keep them. Check them, or keep them as they are."
}

fun itemsLabel(n: Int) = "$n ${if (n == 1) "item" else "items"}"

/** "Tue 10 Nov · 4 items" */
fun dayItemsTitle(date: LocalDate, items: Int) =
    "${weekdayName(date).take(3)} ${date.day} ${MonthLong[date.month.ordinal].take(3)} · ${itemsLabel(items)}"

/** Number on the calendar cell; nothing when the day has a single item. */
fun countBadge(items: Int): String? = if (items > 1) items.toString() else null

fun filledToast(count: Int) = "$count ${daysWord(count)} filled · Check them in the calendar"

fun removedLabel(count: Int) = "$count ${daysWord(count)} removed"

fun keptLabel(count: Int) = "$count ${daysWord(count)} kept"

fun dismissKey(projectId: String, date: LocalDate) = "$projectId|$date"
