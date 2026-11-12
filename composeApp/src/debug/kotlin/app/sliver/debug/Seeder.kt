package app.sliver.debug

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.db.Film
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.SaveClipRequest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.hours

const val SEED_VARIANTS = 6
const val SEED_SECOND_PROJECT = "Lisbon trip"
private const val SECOND_PROJECT_DAYS = 9

data class SeedDay(val date: LocalDate, val variant: Int)

object SeedPlan {
    /** Roughly one day in 13 stays empty; the memory-card dates (1 year, 100 days, 1 month ago) and today are always filled. */
    fun days(today: LocalDate, days: Int, includeToday: Boolean): List<SeedDay> {
        val start = today.minus(DatePeriod(days = days))
        val last = if (includeToday) today else today.minus(DatePeriod(days = 1))
        val keep = setOf(
            today.minus(DatePeriod(years = 1)), today.minus(DatePeriod(days = 100)), today.minus(DatePeriod(months = 1)), today,
        )
        return generateSequence(start) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= last }.withIndex()
            .filter { (i, date) -> date in keep || i % 13 != 6 }
            .map { (i, date) -> SeedDay(date, (i * 5 + i / 7) % SEED_VARIANTS) }.toList()
    }

    fun secondProjectDays(today: LocalDate): List<SeedDay> =
        (SECOND_PROJECT_DAYS downTo 1).mapIndexed { i, back -> SeedDay(today.minus(DatePeriod(days = back)), i % SEED_VARIANTS) }
}

data class SeedState(val projectIds: List<String> = emptyList(), val previousActive: String? = null)

interface SeedStore {
    fun load(): SeedState
    fun save(state: SeedState)
}

fun interface SeedSources {
    /** Path of a 2 s synthetic video for [variant]. */
    suspend fun source(variant: Int): String
}

/** Fills the app through the same repository code as a real save. */
class Seeder(
    private val projects: ProjectRepository,
    private val days: DayRepository,
    private val films: FilmRepository,
    private val sources: SeedSources,
    private val store: SeedStore,
    private val startRender: (String) -> Unit,
    private val log: (String) -> Unit = {},
) {
    suspend fun seed(today: LocalDate, zone: TimeZone, dayCount: Int, projectName: String, includeToday: Boolean) {
        val previous = store.load().takeIf { it.projectIds.isNotEmpty() }?.also { wipe() }?.previousActive
            ?: projects.active.first()?.id
        val ids = mutableListOf<String>()
        fun remember() = store.save(SeedState(ids.toList(), previous))

        val second = SeedPlan.secondProjectDays(today)
        ids += projects.create(SEED_SECOND_PROJECT, FilmFormat.STORY, second.first().date).id
        remember()
        fill(second, FilmFormat.STORY, zone)

        val main = SeedPlan.days(today, dayCount, includeToday)
        val project = projects.create(projectName, FilmFormat.CINEMA, today.minus(DatePeriod(days = dayCount)))
        ids += project.id
        remember()
        fill(main, FilmFormat.CINEMA, zone)

        if (main.isNotEmpty()) {
            val film = Film(
                id = "seed-film-${project.id.take(8)}", name = projectName, format = FilmFormat.CINEMA, projectId = project.id,
            )
            films.save(film)
            startRender(film.id)
        }
        log("seeded ${main.size + second.size} clips")
    }

    private suspend fun fill(plan: List<SeedDay>, format: FilmFormat, zone: TimeZone) {
        plan.forEachIndexed { i, d ->
            val takenAt = d.date.atStartOfDayIn(zone) + 12.hours
            days.saveClip(
                SaveClipRequest(
                    date = d.date, source = sources.source(d.variant), startMs = 0, speed = 1f, cropY = null,
                    format = format, daySource = DaySource.CAMERA, takenAt = takenAt,
                ),
            )
            if ((i + 1) % 25 == 0) log("saved ${i + 1}/${plan.size}")
        }
    }

    suspend fun wipe() {
        val state = store.load()
        state.projectIds.forEach { projects.delete(it) }
        state.previousActive?.takeIf { id -> projects.projects.first().any { it.id == id } }?.let { projects.setActive(it) }
        store.save(SeedState())
        log("wiped ${state.projectIds.size} projects")
    }
}
