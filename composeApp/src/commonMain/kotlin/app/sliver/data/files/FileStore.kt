package app.sliver.data.files

import kotlinx.datetime.LocalDate

interface FileStore {
    fun dayPath(projectId: String, date: LocalDate): String
    fun thumbPath(projectId: String, date: LocalDate): String
    fun filmPath(filmId: String): String
    fun newTmpPath(timestampMillis: Long): String
    fun exists(path: String): Boolean
    fun delete(path: String): Boolean
    fun move(from: String, to: String): Boolean
    fun freeSpaceBytes(): Long

    /** Deletes tmp files older than [maxAgeMillis]; returns number deleted. */
    fun cleanTmp(nowMillis: Long, maxAgeMillis: Long = TMP_MAX_AGE_MILLIS): Int

    companion object {
        const val TMP_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
