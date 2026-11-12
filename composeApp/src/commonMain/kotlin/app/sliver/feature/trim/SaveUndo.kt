package app.sliver.feature.trim

import app.sliver.core.AppScope
import app.sliver.feature.onboarding.FirstClipTimer
import app.sliver.data.db.Day
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.feature.films.ensureDefaultFilm
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val UNDO_WINDOW_MS = 6000L

/** Outlives the Trim screen: saves a clip (parking the replaced one in tmp) and cleans tmp once the window closes. */
class SaveUndo(private val days: DayRepository, private val files: FileStore, private val films: FilmRepository) {
    private val scope = AppScope

    private class Backup(val video: String, val thumb: String)

    private inner class Pending(val request: SaveClipRequest, val previous: Day?, val backup: Backup?) {
        var timer: Job? = null

        fun cleanup() {
            timer?.cancel()
            if (isTmp(request.source)) files.delete(request.source)
            backup?.let {
                files.delete(it.video)
                files.delete(it.thumb)
            }
        }
    }

    private var pending: Pending? = null

    /** Saves the second; the previous day (if any) is parked in tmp so Undo can restore it. */
    suspend fun save(request: SaveClipRequest): Day {
        pending?.cleanup()
        pending = null
        val previous = days.getDay(request.date)
        val backup = previous?.let { park(it, request.takenAt.toEpochMilliseconds() + 1) }
        val day = try {
            days.saveClip(request)
        } catch (e: Throwable) {
            if (previous != null && backup != null) restore(previous, backup)
            throw e
        }
        FirstClipTimer.clipSaved()
        if (previous == null && days.observeAll().first().size == 1) {
            runCatching { films.ensureDefaultFilm(request.date, request.format) }
        }
        val p = Pending(request, previous, backup)
        pending = p
        p.timer = scope.launch {
            delay(UNDO_WINDOW_MS)
            p.cleanup()
            if (pending === p) pending = null
        }
        return day
    }

    /** Deletes the day row first, then its files; leftovers parked in tmp are reclaimed by the launch-time tmp cleanup. Throws if the row cannot be deleted. */
    suspend fun remove(day: Day) {
        val backup = park(day, day.takenAt.toEpochMilliseconds() + 2)
        try {
            days.delete(day.date)
        } catch (e: Throwable) {
            backup?.let { restore(day, it, save = false) }
            throw e
        }
        if (backup != null) {
            files.delete(backup.video)
            files.delete(backup.thumb)
        } else {
            files.delete(day.file)
            files.delete(day.thumb)
        }
    }

    private fun park(day: Day, stamp: Long): Backup? {
        val video = files.newTmpPath(stamp)
        val thumb = "$video.jpg"
        return if (files.move(day.file, video) && files.move(day.thumb, thumb)) Backup(video, thumb) else null
    }

    private suspend fun restore(day: Day, backup: Backup, save: Boolean = true) {
        files.move(backup.video, day.file)
        files.move(backup.thumb, day.thumb)
        if (save) days.save(day)
    }
}
