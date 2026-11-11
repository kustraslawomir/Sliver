package app.sliver

import app.sliver.data.FilmFormat
import app.sliver.data.db.Project
import app.sliver.data.db.ProjectStats
import app.sliver.feature.projects.canLeaveActive
import app.sliver.feature.projects.cleanProjectName
import app.sliver.feature.projects.nextProjectName
import app.sliver.feature.projects.projectMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class ProjectsLogicTest {
    private fun project(id: String, archived: Boolean = false, format: FilmFormat = FilmFormat.CINEMA) =
        Project(id, id, format, LocalDate(2026, 6, 21), createdAt = Instant.fromEpochSeconds(0), archived = archived)

    @Test
    fun defaultNameSkipsTakenNumbers() {
        assertEquals("Project 1", nextProjectName(emptyList()))
        assertEquals("Project 3", nextProjectName(listOf("My 2026", "Garden")))
        assertEquals("Project 4", nextProjectName(listOf("Project 2", "Project 3", "x")))
    }

    @Test
    fun nameIsTrimmedAndNonBlank() {
        assertEquals("Lisbon", cleanProjectName("  Lisbon "))
        assertNull(cleanProjectName("   "))
    }

    @Test
    fun metaLine() {
        val stats = ProjectStats("a", 129, LocalDate(2026, 6, 21), null)
        assertEquals("CINEMA 16:9 · 129 CLIPS · SINCE 21 JUN", projectMeta(project("a"), stats))
        assertEquals("STORY 9:16 · 0 CLIPS · SINCE 21 JUN", projectMeta(project("a", format = FilmFormat.STORY), null))
    }

    @Test
    fun cannotLeaveTheLastOpenProject() {
        val a = project("a")
        assertFalse(canLeaveActive(a, listOf(a)))
        assertFalse(canLeaveActive(a, listOf(a, project("b", archived = true))))
        assertTrue(canLeaveActive(a, listOf(a, project("b"))))
        assertTrue(canLeaveActive(project("z", archived = true), listOf(a, project("z", archived = true))))
    }
}
