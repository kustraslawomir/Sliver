package app.sliver.data.repo

import app.sliver.data.FilmFormat
import app.sliver.data.backup.DriveTrashQueue
import app.sliver.data.db.DayDao
import app.sliver.data.db.FilmDao
import app.sliver.data.db.Project
import app.sliver.data.db.ProjectStats
import app.sliver.data.files.FileStore
import app.sliver.data.db.ProjectDao
import app.sliver.data.settings.SettingsRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

interface ProjectRepository {
    val projects: Flow<List<Project>>
    val active: Flow<Project?>

    /** Id of the active project; "My <year>" is created on first use when none exists (fresh install). */
    suspend fun activeId(): String
    suspend fun create(name: String, format: FilmFormat, startDate: LocalDate): Project
    suspend fun setActive(id: String)
    /** Adds a project recreated from a backup without making it active. */
    suspend fun restore(project: Project)
    suspend fun rename(id: String, name: String)
    suspend fun setFormat(id: String, format: FilmFormat)
    suspend fun setArchived(id: String, archived: Boolean)

    /** Removes the project with its clips, films and local files; Drive copies are queued for trash. */
    suspend fun delete(id: String)
    val stats: Flow<Map<String, ProjectStats>>
}

@OptIn(ExperimentalUuidApi::class)
class ProjectRepositoryImpl(
    private val dao: ProjectDao,
    private val settings: SettingsRepository,
    private val days: DayDao,
    private val films: FilmDao,
    private val files: FileStore,
    private val trash: DriveTrashQueue,
) : ProjectRepository {
    override val stats = dao.observeStats().map { list -> list.associateBy { it.projectId } }

    override val projects = dao.observeAll()

    override val active: Flow<Project?> = flow {
        ensureDefault()
        emitAll(
            combine(settings.settings.map { it.activeProjectId }, projects) { id, all ->
                all.firstOrNull { it.id == id } ?: all.firstOrNull { !it.archived } ?: all.firstOrNull()
            },
        )
    }

    override suspend fun activeId(): String = active.first()!!.id

    private suspend fun ensureDefault() {
        if (dao.count() > 0) return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        create("My ${today.year}", settings.settings.first().newFilmFormat, today)
    }

    override suspend fun create(name: String, format: FilmFormat, startDate: LocalDate): Project {
        val project = Project(Uuid.random().toString(), name, format, startDate, createdAt = Clock.System.now())
        dao.upsert(project)
        settings.update { it.copy(activeProjectId = project.id) }
        return project
    }

    override suspend fun restore(project: Project) = dao.upsert(project)

    override suspend fun setActive(id: String) {
        settings.update { it.copy(activeProjectId = id) }
    }

    override suspend fun rename(id: String, name: String) = dao.rename(id, name)
    override suspend fun setFormat(id: String, format: FilmFormat) = dao.setFormat(id, format)
    override suspend fun setArchived(id: String, archived: Boolean) {
        dao.setArchived(id, archived)
        if (archived && activeId() == id) {
            projects.first().firstOrNull { !it.archived && it.id != id }?.let { setActive(it.id) }
        }
    }

    override suspend fun delete(id: String) {
        days.listOf(id).forEach { day ->
            files.delete(day.file)
            files.delete(day.thumb)
            day.driveFileId?.let { trash.enqueue(it) }
        }
        films.listOf(id).forEach { film -> film.renderPath?.let(files::delete) }
        days.deleteAll(id)
        films.deleteAll(id)
        dao.delete(id)
        settings.update { if (it.activeProjectId == id) it.copy(activeProjectId = null) else it }
    }
}
