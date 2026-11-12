package app.sliver.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.sliver.data.FilmFormat
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

    @Query("UPDATE project SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE project SET format = :format WHERE id = :id")
    suspend fun setFormat(id: String, format: FilmFormat)

    @Query("UPDATE project SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query(
        "SELECT projectId, COUNT(*) AS clips, MIN(date) AS since, " +
            "(SELECT thumb FROM day d2 WHERE d2.projectId = d.projectId AND d2.draft = 0 ORDER BY d2.date DESC LIMIT 1) AS cover " +
            "FROM day d WHERE draft = 0 GROUP BY projectId",
    )
    fun observeStats(): Flow<List<ProjectStats>>
}

data class ProjectStats(val projectId: String, val clips: Int, val since: LocalDate?, val cover: String?)

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE projectId = :projectId AND date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(projectId: String, from: LocalDate, to: LocalDate): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND date = :date")
    fun observeByDate(projectId: String, date: LocalDate): Flow<Day?>

    @Query("SELECT * FROM day WHERE luma < 0")
    suspend fun withoutLight(): List<Day>

    @Query("UPDATE day SET luma = :luma, tone = :tone WHERE projectId = :projectId AND date = :date")
    suspend fun setLight(projectId: String, date: LocalDate, luma: Float, tone: Int)

    @Query("SELECT * FROM day WHERE projectId = :projectId AND date = :date")
    suspend fun getByDate(projectId: String, date: LocalDate): Day?

    @Query("SELECT COUNT(*) FROM day WHERE projectId = :projectId AND draft = 0")
    fun observeCount(projectId: String): Flow<Int>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND draft = 0 ORDER BY date")
    fun observeAll(projectId: String): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE projectId = :projectId")
    suspend fun listOf(projectId: String): List<Day>

    @Query("SELECT * FROM day WHERE draft = 0")
    fun observeEverything(): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND draft = 1 ORDER BY date")
    fun observeDrafts(projectId: String): Flow<List<Day>>

    @Query("SELECT * FROM day WHERE projectId = :projectId AND draft = 1 AND importRunId = :runId")
    suspend fun draftsOfRun(projectId: String, runId: String): List<Day>

    @Query("DELETE FROM day WHERE projectId = :projectId")
    suspend fun deleteAll(projectId: String)

    @Upsert
    suspend fun upsert(day: Day)

    @Query("DELETE FROM day WHERE projectId = :projectId AND date = :date")
    suspend fun delete(projectId: String, date: LocalDate)
}

@Dao
interface FilmDao {
    @Query("SELECT * FROM film WHERE projectId = :projectId ORDER BY name")
    fun observeAll(projectId: String): Flow<List<Film>>

    @Query("SELECT * FROM film")
    fun observeEverything(): Flow<List<Film>>

    @Query("SELECT * FROM film WHERE id = :id")
    fun observeById(id: String): Flow<Film?>

    @Query("SELECT * FROM film WHERE id = :id")
    suspend fun getById(id: String): Film?

    @Query("SELECT COUNT(*) FROM film WHERE projectId = :projectId")
    fun observeCount(projectId: String): Flow<Int>

    @Query("SELECT * FROM film WHERE projectId = :projectId")
    suspend fun listOf(projectId: String): List<Film>

    @Query("DELETE FROM film WHERE projectId = :projectId")
    suspend fun deleteAll(projectId: String)

    @Upsert
    suspend fun upsert(film: Film)

    @Query("DELETE FROM film WHERE id = :id")
    suspend fun delete(id: String)
}
