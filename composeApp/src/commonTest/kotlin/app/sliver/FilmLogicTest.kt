package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.db.Day
import app.sliver.feature.films.TeaserState
import app.sliver.feature.films.buildFilmPlan
import app.sliver.feature.films.formatRuntime
import app.sliver.feature.films.plan
import app.sliver.feature.films.subtitle
import app.sliver.feature.films.spanText
import app.sliver.feature.films.clipsText
import app.sliver.feature.films.teaserState
import app.sliver.feature.films.yearFilm
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FilmLogicTest {
    private fun day(y: Int, m: Int, d: Int) = Day(
        LocalDate(y, m, d), "f$m-$d", "t", DaySource.CAMERA,
        takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val days = listOf(day(2026, 3, 5), day(2026, 1, 2), day(2025, 12, 31), day(2026, 1, 9), day(2027, 1, 1))

    @Test fun planIsEveryFilledDayInOrder() {
        val p = buildFilmPlan(days)
        assertEquals(listOf("f12-31", "f1-2", "f1-9", "f3-5", "f1-1"), p.files)
        assertEquals(5, p.clips)
    }

    @Test fun filmIncludesEveryFilledDayOfItsProject() {
        val film = yearFilm(2026, FilmFormat.CINEMA)
        assertEquals(days.size, film.plan(days).clips)
        assertEquals(days.size, film.copy(rangeType = FilmRangeType.CUSTOM, rangeFrom = LocalDate(2026, 1, 1), rangeTo = LocalDate(2026, 1, 9)).plan(days).clips)
    }

    @Test fun emptyPlan() {
        assertEquals(0, buildFilmPlan(emptyList()).clips)
        assertEquals("No clips yet", buildFilmPlan(emptyList()).subtitle())
    }

    @Test fun runtimeAndSubtitle() {
        assertEquals("1:05", formatRuntime(65))
        assertEquals("0:00", formatRuntime(0))
        assertEquals("1 Jan 2027", buildFilmPlan(listOf(day(2027, 1, 1))).spanText())
        assertEquals("2 Jan – 5 Mar 2026 · 3 clips", buildFilmPlan(days.filter { it.date.year == 2026 }).subtitle())
        assertEquals("31 Dec 2025 – 1 Jan 2027 · 5 clips", buildFilmPlan(days).subtitle())
        assertEquals("1 clip", clipsText(1))
    }

    @Test fun teaserWindow() {
        assertEquals(TeaserState.HIDDEN, teaserState(LocalDate(2026, 11, 30)))
        assertEquals(TeaserState.LOCKED, teaserState(LocalDate(2026, 12, 1)))
        assertEquals(TeaserState.LOCKED, teaserState(LocalDate(2026, 12, 25)))
        assertEquals(TeaserState.UNLOCKED, teaserState(LocalDate(2026, 12, 26)))
    }
}
