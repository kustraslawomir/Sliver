package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.LookPreset
import app.sliver.data.db.Day
import app.sliver.feature.films.isStale
import app.sliver.feature.films.monthsText
import app.sliver.feature.films.yearFilm
import app.sliver.feature.films.yearStats
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YearLogicTest {
    private fun day(m: Int, d: Int, look: LookPreset = LookPreset.NATURAL, created: Long = 0, year: Int = 2026) = Day(
        date = LocalDate(year, m, d), file = "f", thumb = "t", source = DaySource.CAMERA,
        look = LookParams(preset = look), takenAt = Instant.fromEpochSeconds(0), createdAt = Instant.fromEpochSeconds(created),
    )

    @Test fun statsCountOnlyThatYear() {
        val s = yearStats(2026, listOf(day(1, 1), day(1, 2), day(3, 1), day(1, 1, year = 2025)), emptyList())
        assertEquals(3, s.seconds)
        assertEquals(listOf(1), s.topMonths)
    }

    @Test fun tiedMonthsAreAllReported() {
        val s = yearStats(2026, listOf(day(2, 1), day(5, 1), day(5, 2), day(2, 2)), emptyList())
        assertEquals(listOf(2, 5), s.topMonths)
        assertEquals("February · May", monthsText(s.topMonths))
    }

    @Test fun favouriteLookAndFilms() {
        val rendered = yearFilm(2026, FilmFormat.CINEMA).copy(renderPath = "p")
        val s = yearStats(
            2026,
            listOf(day(1, 1, LookPreset.NOIR), day(1, 2, LookPreset.NOIR), day(1, 3)),
            listOf(rendered, yearFilm(2026, FilmFormat.STORY, false)),
        )
        assertEquals(LookPreset.NOIR, s.favouriteLook)
        assertEquals(1, s.films)
    }

    @Test fun emptyYear() {
        val s = yearStats(2026, emptyList(), emptyList())
        assertEquals(0, s.seconds)
        assertNull(s.favouriteLook)
        assertEquals("None yet", monthsText(s.topMonths))
    }

    @Test fun staleness() {
        val film = yearFilm(2026, FilmFormat.CINEMA)
        assertTrue(film.isStale(emptyList()))
        val rendered = film.copy(renderPath = "p", renderedAt = Instant.fromEpochSeconds(100))
        assertFalse(rendered.isStale(listOf(day(1, 1, created = 50))))
        assertTrue(rendered.isStale(listOf(day(1, 1, created = 150))))
    }
}
