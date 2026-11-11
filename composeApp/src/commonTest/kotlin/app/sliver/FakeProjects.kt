package app.sliver

import app.sliver.data.FilmFormat
import app.sliver.data.db.Project
import app.sliver.data.repo.ProjectRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FakeProjects(initial: List<Project> = listOf(project("p1", "My 2025")), active: String = initial.first().id) : ProjectRepository {
    val all = MutableStateFlow(initial)
    val activeId = MutableStateFlow(active)
    override val projects = all
    override val active = combine(all, activeId) { list, id -> list.firstOrNull { it.id == id } }
    override val stats = all.map { emptyMap<String, app.sliver.data.db.ProjectStats>() }

    override suspend fun activeId() = activeId.value
    override suspend fun create(name: String, format: FilmFormat, startDate: LocalDate) = project("new-${all.value.size}", name).also {
        all.value = all.value + it
        activeId.value = it.id
    }
    override suspend fun restore(project: Project) { all.value = all.value + project }
    override suspend fun setActive(id: String) { activeId.value = id }
    override suspend fun rename(id: String, name: String) = Unit
    override suspend fun setFormat(id: String, format: FilmFormat) = Unit
    override suspend fun setArchived(id: String, archived: Boolean) = Unit
    override suspend fun delete(id: String) = Unit
}

fun project(id: String, name: String) =
    Project(id, name, FilmFormat.CINEMA, LocalDate(2025, 1, 1), createdAt = Instant.fromEpochMilliseconds(0))
