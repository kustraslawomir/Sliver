package app.sliver.data.repo

import app.sliver.data.FilmFormat
import app.sliver.data.db.Project
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
}

@OptIn(ExperimentalUuidApi::class)
class ProjectRepositoryImpl(
    private val dao: ProjectDao,
    private val settings: SettingsRepository,
) : ProjectRepository {
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

    override suspend fun setActive(id: String) {
        settings.update { it.copy(activeProjectId = id) }
    }
}
