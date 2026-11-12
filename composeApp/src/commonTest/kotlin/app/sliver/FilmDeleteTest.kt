package app.sliver

import app.sliver.data.FilmFormat
import app.sliver.data.db.Film
import app.sliver.data.files.FileStore
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.films.FilmDeleter
import app.sliver.feature.films.FilmRenderer
import app.sliver.feature.films.RenderState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FilmDeleteTest {
    private class Films(vararg initial: Film) : FilmRepository {
        val rows = initial.toMutableList()
        override fun observeAll(): Flow<List<Film>> = MutableStateFlow(rows)
        override fun observeAllProjects(): Flow<List<Film>> = MutableStateFlow(rows)
        override fun observeFilm(id: String): Flow<Film?> = emptyFlow()
        override fun observeCount() = MutableStateFlow(rows.size)
        override suspend fun getFilm(id: String) = rows.firstOrNull { it.id == id }
        override suspend fun save(film: Film) { rows.removeAll { it.id == film.id }; rows += film }
        override suspend fun delete(id: String) { rows.removeAll { it.id == id } }
    }

    private class Files(vararg initial: String) : FileStore {
        val present = initial.toMutableSet()
        private var n = 0
        override fun dayPath(projectId: String, date: LocalDate) = ""
        override fun thumbPath(projectId: String, date: LocalDate) = ""
        override fun filmPath(filmId: String) = "films/$filmId.mp4"
        override fun newTmpPath(timestampMillis: Long) = "tmp/${n++}"
        override fun exists(path: String) = path in present
        override fun delete(path: String) = present.remove(path)
        override fun move(from: String, to: String) = present.remove(from).also { if (it) present += to }
        override fun freeSpaceBytes() = Long.MAX_VALUE
        override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long) = 0
    }

    private class Renderer : FilmRenderer {
        val cancelled = mutableListOf<String>()
        override fun start(filmId: String) = Unit
        override fun cancel(filmId: String) { cancelled += filmId }
        override fun state(filmId: String): Flow<RenderState> = emptyFlow()
    }

    private val film = Film(id = "f1", name = "My 2026", format = FilmFormat.CINEMA, live = true, renderPath = "films/f1.mp4")

    @Test
    fun undoRestoresRowAndFile() = runTest {
        val films = Films(film)
        val files = Files("films/f1.mp4")
        val renderer = Renderer()
        val deleter = FilmDeleter(films, files, renderer, backgroundScope, 6000)
        deleter.delete(film)
        assertTrue(films.rows.isEmpty())
        assertFalse(files.exists("films/f1.mp4"))
        assertEquals(listOf("f1"), renderer.cancelled)
        assertTrue(deleter.undo("f1"))
        assertEquals(listOf(film), films.rows)
        assertTrue(files.exists("films/f1.mp4"))
        advanceTimeBy(7000); runCurrent()
        assertTrue(files.exists("films/f1.mp4"))
    }

    @Test
    fun withoutUndoRowAndFileAreGone() = runTest {
        val films = Films(film)
        val files = Files("films/f1.mp4")
        val deleter = FilmDeleter(films, files, Renderer(), backgroundScope, 6000)
        deleter.delete(film)
        advanceTimeBy(5900); runCurrent()
        assertTrue(files.present.isNotEmpty())
        advanceTimeBy(200); runCurrent()
        assertTrue(files.present.isEmpty())
        assertTrue(films.rows.isEmpty())
        assertFalse(deleter.undo("f1"))
    }
}
