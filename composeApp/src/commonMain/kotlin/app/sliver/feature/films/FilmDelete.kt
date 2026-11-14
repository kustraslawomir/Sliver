package app.sliver.feature.films

import app.sliver.core.AppScope
import app.sliver.core.ui.UndoToasts
import app.sliver.data.db.Film
import app.sliver.data.files.FileStore
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.trim.UNDO_WINDOW_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Deleting a film acts now and can be undone for [windowMs]: the row goes immediately, the rendered file is parked in tmp
 * and removed when the window closes. Clips are never touched; the next backup drops the film from the Drive manifest.
 */
class FilmDeleter(
    private val films: FilmRepository,
    private val files: FileStore,
    private val renderer: FilmRenderer,
    private val scope: CoroutineScope = AppScope,
    private val windowMs: Long = UNDO_WINDOW_MS,
) {
    private class Pending(val film: Film, val parked: String?) {
        var timer: Job? = null
    }

    private val pending = mutableMapOf<String, Pending>()

    suspend fun delete(film: Film) {
        renderer.cancel(film.id)
        val parked = film.renderPath?.let { path ->
            files.newTmpPath(Clock.System.now().toEpochMilliseconds()).takeIf { files.move(path, it) }
        }
        try {
            films.delete(film.id)
        } catch (e: Throwable) {
            parked?.let { files.move(it, film.renderPath!!) }
            throw e
        }
        val p = Pending(film, parked)
        pending[film.id] = p
        p.timer = scope.launch {
            delay(windowMs)
            finish(p)
        }
        UndoToasts.show("Film deleted") { scope.launch { undo(film.id) } }
    }

    suspend fun undo(id: String): Boolean {
        val p = pending.remove(id) ?: return false
        p.timer?.cancel()
        p.parked?.let { files.move(it, p.film.renderPath!!) }
        films.save(p.film)
        return true
    }

    private fun finish(p: Pending) {
        if (pending[p.film.id] === p) pending.remove(p.film.id)
        p.parked?.let(files::delete)
    }
}
