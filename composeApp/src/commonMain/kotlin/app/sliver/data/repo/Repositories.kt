package app.sliver.data.repo

import app.sliver.data.db.Day
import app.sliver.data.db.DayDao
import app.sliver.data.db.Film
import app.sliver.data.db.FilmDao
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface DayRepository {
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>>
    fun observeAll(): Flow<List<Day>>
    fun observeDay(date: LocalDate): Flow<Day?>
    fun observeCount(): Flow<Int>
    suspend fun getDay(date: LocalDate): Day?
    suspend fun save(day: Day)
    suspend fun delete(date: LocalDate)
}

interface FilmRepository {
    fun observeAll(): Flow<List<Film>>
    fun observeFilm(id: String): Flow<Film?>
    fun observeCount(): Flow<Int>
    suspend fun getFilm(id: String): Film?
    suspend fun save(film: Film)
    suspend fun delete(id: String)
}

class DayRepositoryImpl(private val dao: DayDao) : DayRepository {
    override fun observeRange(from: LocalDate, to: LocalDate) = dao.observeRange(from, to)
    override fun observeAll() = dao.observeAll()
    override fun observeDay(date: LocalDate) = dao.observeByDate(date)
    override fun observeCount() = dao.observeCount()
    override suspend fun getDay(date: LocalDate) = dao.getByDate(date)
    override suspend fun save(day: Day) = dao.upsert(day)
    override suspend fun delete(date: LocalDate) = dao.delete(date)
}

class FilmRepositoryImpl(private val dao: FilmDao) : FilmRepository {
    override fun observeAll() = dao.observeAll()
    override fun observeFilm(id: String) = dao.observeById(id)
    override fun observeCount() = dao.observeCount()
    override suspend fun getFilm(id: String) = dao.getById(id)
    override suspend fun save(film: Film) = dao.upsert(film)
    override suspend fun delete(id: String) = dao.delete(id)
}
