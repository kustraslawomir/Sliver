package app.sliver.data.files

import kotlinx.datetime.LocalDate

/** A recording file in tmp: [startMs] from its name (rec_<start>.mp4), [endMs] = last modified (when recording stopped). */
data class TmpRecording(val path: String, val startMs: Long, val endMs: Long)

interface FileStore {
    fun dayPath(projectId: String, date: LocalDate): String
    fun thumbPath(projectId: String, date: LocalDate): String
    fun filmPath(filmId: String): String
    fun newTmpPath(timestampMillis: Long): String
    fun exists(path: String): Boolean
    fun delete(path: String): Boolean
    fun move(from: String, to: String): Boolean
    fun freeSpaceBytes(): Long

    /** Where a kept camera recording lives (app files, never cleaned by the system or [cleanTmp]). */
    fun originalPath(stampMillis: Long): String = newTmpPath(stampMillis).replace("rec_", "orig_")

    /** Camera recordings still in tmp (rec_*.mp4 without a parked thumbnail next to them). */
    fun tmpRecordings(): List<TmpRecording> = emptyList()

    /** Deletes tmp files older than [maxAgeMillis]; returns number deleted. */
    fun cleanTmp(nowMillis: Long, maxAgeMillis: Long = TMP_MAX_AGE_MILLIS): Int

    companion object {
        const val TMP_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
