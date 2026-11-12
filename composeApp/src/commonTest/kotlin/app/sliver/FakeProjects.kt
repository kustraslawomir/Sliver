package app.sliver

import app.sliver.data.FilmFormat
import app.sliver.data.backup.NoTombstones
import app.sliver.data.backup.TombstoneKind
import app.sliver.data.backup.TombstoneStore
import app.sliver.data.db.Project
import app.sliver.data.repo.ProjectRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FakeProjects(
    initial: List<Project> = listOf(project("p1", "My 2025")),
    active: String = initial.firstOrNull()?.id.orEmpty(),
    private val tombstones: TombstoneStore = NoTombstones,
    private val now: () -> Long = { 0L },
) : ProjectRepository {
    val all = MutableStateFlow(initial)
    val activeId = MutableStateFlow(active)
    override val projects = all
    override val active = combine(all, activeId) { list, id -> list.firstOrNull { it.id == id } }
    override val stats = all.map { emptyMap<String, app.sliver.data.db.ProjectStats>() }

    override suspend fun activeId() = activeId.value
    override suspend fun create(name: String, format: FilmFormat, startDate: LocalDate) = project("new-${all.value.size}", name).copy(updatedAt = now()).also {
        all.value = all.value + it
        activeId.value = it.id
    }
    override suspend fun restore(project: Project) { all.value = all.value.filter { it.id != project.id } + project }
    override suspend fun setActive(id: String) { activeId.value = id }
    private fun edit(id: String, change: (Project) -> Project) {
        all.value = all.value.map { if (it.id == id) change(it).copy(updatedAt = now(), updatedBy = "") else it }
    }
    override suspend fun rename(id: String, name: String) = edit(id) { it.copy(name = name) }
    override suspend fun setFormat(id: String, format: FilmFormat) = edit(id) { it.copy(format = format) }
    override suspend fun setArchived(id: String, archived: Boolean) = edit(id) { it.copy(archived = archived) }
    override suspend fun delete(id: String) {
        tombstones.record(TombstoneKind.PROJECT, id, "", now())
        removeRemote(id)
    }
    override suspend fun removeRemote(id: String) { all.value = all.value.filter { it.id != id } }
}

fun project(id: String, name: String) =
    Project(id, name, FilmFormat.CINEMA, LocalDate(2025, 1, 1), createdAt = Instant.fromEpochMilliseconds(0))
