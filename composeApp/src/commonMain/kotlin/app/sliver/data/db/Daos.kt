package app.sliver.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY createdAt")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun getById(id: String): Project?

    @Query("SELECT COUNT(*) FROM project")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(project: Project)

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE projectId = :projectId AND date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(projectId: String, from: LocalDate, to: LocalDate): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND date = :date")
    fun observeByDate(projectId: String, date: LocalDate): Flow<Day?>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND date = :date")
    suspend fun getByDate(projectId: String, date: LocalDate): Day?

    @Query("SELECT COUNT(*) FROM day WHERE projectId = :projectId")
    fun observeCount(projectId: String): Flow<Int>

    @Query("SELECT * FROM day WHERE projectId = :projectId ORDER BY date")
    fun observeAll(projectId: String): Flow<List<Day>>

    @Upsert
    suspend fun upsert(day: Day)

    @Query("DELETE FROM day WHERE projectId = :projectId AND date = :date")
    suspend fun delete(projectId: String, date: LocalDate)
}

@Dao
interface FilmDao {
    @Query("SELECT * FROM film WHERE projectId = :projectId ORDER BY name")
    fun observeAll(projectId: String): Flow<List<Film>>

    @Query("SELECT * FROM film WHERE id = :id")
    fun observeById(id: String): Flow<Film?>

    @Query("SELECT * FROM film WHERE id = :id")
    suspend fun getById(id: String): Film?

    @Query("SELECT COUNT(*) FROM film WHERE projectId = :projectId")
    fun observeCount(projectId: String): Flow<Int>

    @Upsert
    suspend fun upsert(film: Film)

    @Query("DELETE FROM film WHERE id = :id")
    suspend fun delete(id: String)
}
