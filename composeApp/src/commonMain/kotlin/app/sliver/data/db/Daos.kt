package app.sliver.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<Day?>

    @Query("SELECT * FROM day WHERE date = :date")
    suspend fun getByDate(date: LocalDate): Day?

    @Query("SELECT COUNT(*) FROM day")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM day ORDER BY date")
    fun observeAll(): Flow<List<Day>>

    @Upsert
    suspend fun upsert(day: Day)

    @Query("DELETE FROM day WHERE date = :date")
    suspend fun delete(date: LocalDate)
}

@Dao
interface FilmDao {
    @Query("SELECT * FROM film ORDER BY name")
    fun observeAll(): Flow<List<Film>>

    @Query("SELECT * FROM film WHERE id = :id")
    fun observeById(id: String): Flow<Film?>

    @Query("SELECT * FROM film WHERE id = :id")
    suspend fun getById(id: String): Film?

    @Query("SELECT COUNT(*) FROM film")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(film: Film)

    @Query("DELETE FROM film WHERE id = :id")
    suspend fun delete(id: String)
}
