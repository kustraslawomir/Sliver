package app.sliver.debug

import app.sliver.FakeProjects
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Project
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveClipRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeederTest {
    private val today = LocalDate(2026, 11, 12)

    @Test
    fun planLeavesAboutOneDayInThirteenEmptyButKeepsMemoryDates() {
        val plan = SeedPlan.days(today, 365, includeToday = false)
        val dates = plan.map { it.date }.toSet()
        assertTrue(today.minus(DatePeriod(years = 1)) in dates)
        assertTrue(today.minus(DatePeriod(months = 1)) in dates)
        assertTrue(today.minus(DatePeriod(days = 100)) in dates)
        assertTrue(today !in dates)
        val empty = 365 - plan.size
        assertTrue(empty in 25..30, "empty=$empty")
        assertEquals(SEED_VARIANTS, plan.map { it.variant }.toSet().size)
    }

    @Test
    fun todayOnlyWhenAsked() {
        assertTrue(today in SeedPlan.days(today, 30, includeToday = true).map { it.date })
    }

    @Test
    fun seedGoesThroughSaveClipThenWipeRestoresPreviousProject() = runTest {
        val projects = SeedProjects()
        val days = RecordingDays()
        val films = RecordingFilms()
        var stored = SeedState()
        var rendered: String? = null
        val seeder = Seeder(
            projects, days, films, { "/seed/$it.mp4" },
            object : SeedStore {
                override fun load() = stored
                override fun save(state: SeedState) { stored = state }
            },
            { rendered = it },
        )
        seeder.seed(today, TimeZone.UTC, 60, "My 2026", includeToday = true)

        assertEquals(listOf(SEED_SECOND_PROJECT, "My 2026"), projects.all.value.drop(1).map { it.name })
        assertEquals(FilmFormat.STORY, days.requests.first().format)
        assertEquals(9, days.requests.count { it.format == FilmFormat.STORY })
        assertTrue(days.requests.count { it.format == FilmFormat.CINEMA } > 50)
        assertEquals(1, films.saved.size)
        assertEquals(films.saved.single().id, rendered)
        assertEquals(projects.all.value.last().id, projects.activeId.value)

        seeder.wipe()
        assertEquals(listOf("p1"), projects.all.value.map { it.id })
        assertEquals("p1", projects.activeId.value)
        assertEquals(SeedState(), stored)
    }
}

private class SeedProjects(private val fake: FakeProjects = FakeProjects()) : app.sliver.data.repo.ProjectRepository by fake {
    val all get() = fake.all
    val activeId get() = fake.activeId
    override suspend fun delete(id: String) { fake.all.value = fake.all.value.filterNot { it.id == id } }
}

private class RecordingDays : DayRepository {
    val requests = mutableListOf<SaveClipRequest>()
    override suspend fun saveClip(request: SaveClipRequest): Day {
        requests += request
        return Day(request.date, request.source, "", request.daySource, takenAt = request.takenAt, createdAt = request.takenAt)
    }
    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>> = emptyFlow()
    override fun observeAll(): Flow<List<Day>> = emptyFlow()
    override fun observeDay(date: LocalDate): Flow<Day?> = emptyFlow()
    override fun observeCount(): Flow<Int> = emptyFlow()
    override suspend fun getDay(date: LocalDate): Day? = null
    override suspend fun getDay(projectId: String, date: LocalDate): Day? = null
    override fun observeAllProjects(): Flow<List<Day>> = emptyFlow()
    override suspend fun save(day: Day) = Unit
    override suspend fun delete(date: LocalDate) = Unit
}

private class RecordingFilms : FilmRepository {
    val saved = mutableListOf<Film>()
    override fun observeAll(): Flow<List<Film>> = MutableStateFlow(saved)
    override fun observeAllProjects(): Flow<List<Film>> = emptyFlow()
    override fun observeFilm(id: String): Flow<Film?> = emptyFlow()
    override fun observeCount(): Flow<Int> = emptyFlow()
    override suspend fun getFilm(id: String): Film? = null
    override suspend fun save(film: Film) { saved += film }
    override suspend fun delete(id: String) = Unit
}
