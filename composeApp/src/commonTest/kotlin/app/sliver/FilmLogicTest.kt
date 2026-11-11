package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.feature.films.TeaserState
import app.sliver.feature.films.buildFilmPlan
import app.sliver.feature.films.formatRuntime
import app.sliver.feature.films.plan
import app.sliver.feature.films.rangeText
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

    @Test fun planKeepsRangeInOrderSkippingOpenDays() {
        val p = buildFilmPlan(days, LocalDate(2026, 1, 1), LocalDate(2026, 12, 31))
        assertEquals(listOf("f1-2", "f1-9", "f3-5"), p.files)
        assertEquals(3, p.clips)
    }

    @Test fun rangeBoundsAreInclusive() {
        assertEquals(2, buildFilmPlan(days, LocalDate(2026, 1, 2), LocalDate(2026, 1, 9)).clips)
    }

    @Test fun emptyPlan() {
        assertEquals(0, buildFilmPlan(emptyList(), null, null).clips)
    }

    @Test fun yearFilmPlan() {
        assertEquals(3, yearFilm(2026, FilmFormat.CINEMA).plan(days).clips)
    }

    @Test fun runtimeAndRangeText() {
        assertEquals("1:05", formatRuntime(65))
        assertEquals("0:00", formatRuntime(0))
        assertEquals("2026", yearFilm(2026, FilmFormat.STORY).rangeText())
    }

    @Test fun teaserWindow() {
        assertEquals(TeaserState.HIDDEN, teaserState(LocalDate(2026, 11, 30)))
        assertEquals(TeaserState.LOCKED, teaserState(LocalDate(2026, 12, 1)))
        assertEquals(TeaserState.LOCKED, teaserState(LocalDate(2026, 12, 25)))
        assertEquals(TeaserState.UNLOCKED, teaserState(LocalDate(2026, 12, 26)))
    }
}
