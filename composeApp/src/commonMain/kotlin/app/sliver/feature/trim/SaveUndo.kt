package app.sliver.feature.trim

import app.sliver.core.ui.UndoToastState
import app.sliver.data.db.Day
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveSecondRequest
import app.sliver.feature.films.ensureDefaultFilm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val UNDO_WINDOW_MS = 6000L

/** Outlives the Trim screen: owns the "Saved" toast, the undo, and tmp cleanup once the window closes. */
class SaveUndo(private val days: DayRepository, private val files: FileStore, private val films: FilmRepository) {
    val toast = UndoToastState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private class Backup(val video: String, val thumb: String)

    private inner class Pending(val request: SaveSecondRequest, val previous: Day?, val backup: Backup?) {
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
    suspend fun save(request: SaveSecondRequest, label: String): Day {
        pending?.cleanup()
        pending = null
        val previous = days.getDay(request.date)
        val backup = previous?.let { park(it, request.takenAt.toEpochMilliseconds() + 1) }
        val day = try {
            days.saveSecond(request)
        } catch (e: Throwable) {
            if (previous != null && backup != null) restore(previous, backup)
            throw e
        }
        runCatching { films.ensureDefaultFilm(request.date, request.format) }
        val p = Pending(request, previous, backup)
        pending = p
        p.timer = scope.launch {
            delay(UNDO_WINDOW_MS)
            p.cleanup()
            if (pending === p) pending = null
        }
        toast.show("Saved to $label", onAction = { undo(p) })
        return day
    }

    /** Deletes the day; files are parked in tmp until the undo window closes. */
    suspend fun remove(day: Day, label: String) {
        val backup = park(day, day.takenAt.toEpochMilliseconds() + 2) ?: return
        days.delete(day.date)
        val timer = scope.launch {
            delay(UNDO_WINDOW_MS)
            files.delete(backup.video)
            files.delete(backup.thumb)
        }
        toast.show("Removed $label", onAction = {
            timer.cancel()
            scope.launch { restore(day, backup) }
        })
    }

    private fun undo(p: Pending) {
        p.timer?.cancel()
        scope.launch {
            days.getDay(p.request.date)?.let {
                files.delete(it.file)
                files.delete(it.thumb)
            }
            if (p.previous != null && p.backup != null) restore(p.previous, p.backup) else days.delete(p.request.date)
            p.cleanup()
            if (pending === p) pending = null
        }
    }

    private fun park(day: Day, stamp: Long): Backup? {
        val video = files.newTmpPath(stamp)
        val thumb = "$video.jpg"
        return if (files.move(day.file, video) && files.move(day.thumb, thumb)) Backup(video, thumb) else null
    }

    private suspend fun restore(day: Day, backup: Backup) {
        files.move(backup.video, day.file)
        files.move(backup.thumb, day.thumb)
        days.save(day)
    }
}
