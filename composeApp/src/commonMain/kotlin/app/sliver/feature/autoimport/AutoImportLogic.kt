package app.sliver.feature.autoimport

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
) {
    val progress get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}

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

fun importSummary(done: Int, failed: Int): String =
    "$done ${if (done == 1) "draft" else "drafts"} added" + if (failed > 0) ", $failed couldn't be read" else ""

/** Index of the draft after [date] in [drafts] (sorted by date), wrapping; null when no other draft remains. */
fun <T> nextDraft(drafts: List<T>, date: LocalDate, dateOf: (T) -> LocalDate): T? =
    drafts.firstOrNull { dateOf(it) > date } ?: drafts.firstOrNull { dateOf(it) < date }

fun <T> previousDraft(drafts: List<T>, date: LocalDate, dateOf: (T) -> LocalDate): T? =
    drafts.lastOrNull { dateOf(it) < date } ?: drafts.lastOrNull { dateOf(it) > date }
