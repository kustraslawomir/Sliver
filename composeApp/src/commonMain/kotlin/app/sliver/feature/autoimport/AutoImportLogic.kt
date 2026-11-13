package app.sliver.feature.autoimport

import app.sliver.data.db.Day
import app.sliver.feature.archive.MonthLong
import app.sliver.feature.archive.weekdayName
import app.sliver.feature.gallery.formatDay
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One photo, video or motion photo from the gallery with a real capture time. */
data class MediaEntry(val uri: String, val video: Boolean, val takenAtMs: Long, val durationMs: Long = 0, val motion: Boolean = false)

/** A gallery item offered for a draft day; stored as JSON on the day. */
@Serializable
data class DraftCandidate(val uri: String, val video: Boolean, val durationMs: Long = 0, val motion: Boolean = false)

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
fun rank(entries: List<MediaEntry>): List<MediaEntry> {
    fun len(e: MediaEntry) = if (e.motion) MOTION_NOMINAL_MS else e.durationMs
    val movies = entries.filter { it.video || it.motion }.sortedWith(compareByDescending<MediaEntry> { len(it) }.thenBy { it.takenAtMs }.thenBy { it.uri })
    val stills = entries.filterNot { it.video || it.motion }.sortedWith(compareBy<MediaEntry> { it.takenAtMs }.thenBy { it.uri })
    return movies + stills
}

fun candidatesOf(entries: List<MediaEntry>): List<DraftCandidate> =
    rank(entries).take(MAX_CANDIDATES).map { DraftCandidate(it.uri, it.video, it.durationMs, it.motion) }

/** A video's 1 s window sits in the middle. */
fun middleWindowStartMs(durationMs: Long): Long = ((durationMs - 1000) / 2).coerceAtLeast(0)

private val json = Json { ignoreUnknownKeys = true }

fun encodeCandidates(list: List<DraftCandidate>): String = json.encodeToString(ListSerializer(DraftCandidate.serializer()), list)

fun decodeCandidates(raw: String?): List<DraftCandidate> =
    raw?.let { runCatching { json.decodeFromString(ListSerializer(DraftCandidate.serializer()), it) }.getOrNull() }.orEmpty()

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

fun itemsLabel(n: Int) = "$n ${if (n == 1) "item" else "items"}"

/** "Tue 10 Nov · 4 items" */
fun dayItemsTitle(date: LocalDate, items: Int) =
    "${weekdayName(date).take(3)} ${date.day} ${MonthLong[date.month.ordinal].take(3)} · ${itemsLabel(items)}"

/** Number on the calendar cell; nothing when the day has a single item. */
fun countBadge(items: Int): String? = if (items > 1) items.toString() else null

fun filledToast(count: Int) = "$count ${daysWord(count)} filled · Check them in the calendar"

fun removedLabel(count: Int) = "$count ${daysWord(count)} removed"

fun keptLabel(count: Int) = "$count ${daysWord(count)} kept"
