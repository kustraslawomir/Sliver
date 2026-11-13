package app.sliver.feature.trim

import app.sliver.data.DaySource
import app.sliver.data.db.Day
import app.sliver.data.files.FileStore
import app.sliver.data.files.TmpRecording
import app.sliver.data.repo.DayRepository
import kotlinx.coroutines.flow.first

/** A recording can be trimmed for a while before saving; anything older than this is not that day's recording. */
const val RESCUE_MAX_GAP_MS = 3L * 60 * 60 * 1000

/** Recording stops before the clip is saved; allow a little clock slack for the file's modified time. */
private const val RESCUE_END_SLACK_MS = 10_000L

/**
 * T-glass-v1.7 10.5: pairs camera days that have no kept original with recordings still in tmp. A day takes the latest
 * recording that started before the clip was saved (within [RESCUE_MAX_GAP_MS]) and stopped before it; each recording
 * goes to one day at most.
 */
fun matchOriginals(
    days: List<Day>, recordings: List<TmpRecording>, maxGapMs: Long = RESCUE_MAX_GAP_MS, exists: (String) -> Boolean = { true },
): List<Pair<Day, TmpRecording>> {
    val used = mutableSetOf<String>()
    val out = mutableListOf<Pair<Day, TmpRecording>>()
    for (day in days.filter { it.source == DaySource.CAMERA && (it.original == null || !exists(it.original)) && !it.draft }.sortedByDescending { it.takenAt }) {
        val saved = day.takenAt.toEpochMilliseconds()
        val rec = recordings
            .filter { it.path !in used && it.startMs < saved && saved - it.startMs <= maxGapMs && it.endMs <= saved + RESCUE_END_SLACK_MS }
            .maxByOrNull { it.startMs } ?: continue
        used += rec.path
        out += day to rec
    }
    return out
}

/** Moves a finished camera recording out of tmp into the kept originals; returns its new path, or null if it could not move. */
fun keepOriginal(files: FileStore, recording: String, stampMillis: Long): String? {
    val dest = files.originalPath(stampMillis)
    return dest.takeIf { files.move(recording, it) }
}

/** Runs at launch BEFORE the tmp cleanup, so recordings left in tmp (e.g. today's) can still get Change moment. */
class OriginalRescue(private val days: DayRepository, private val files: FileStore) {
    suspend fun run(): Int {
        val recordings = files.tmpRecordings()
        if (recordings.isEmpty()) return 0
        var n = 0
        for ((day, rec) in matchOriginals(days.observeAllProjects().first(), recordings, exists = files::exists)) {
            val kept = keepOriginal(files, rec.path, rec.startMs) ?: continue
            // DECISION: the 1 s position inside a rescued recording is unknown; Change moment opens at the start.
            days.applyRemote(day.copy(original = kept, originalStartMs = null))
            n++
        }
        return n
    }
}

/** A kept original that is no longer used by [now] (replaced or removed day) is deleted. */
fun dropOriginal(files: FileStore, before: Day?, now: Day?) {
    val old = before?.original ?: return
    if (old != now?.original) files.delete(old)
}
