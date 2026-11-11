package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.db.Day
import app.sliver.feature.films.FilmOptions
import app.sliver.feature.films.FilmPlan
import app.sliver.feature.films.dayIndexAt
import app.sliver.feature.films.fadeAlpha
import app.sliver.feature.films.rangeFor
import app.sliver.feature.films.scrubberTicks
import app.sliver.feature.films.toFilm
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilmBuilderLogicTest {
    private val today = LocalDate(2026, 11, 11)
    private val custom = LocalDate(2026, 2, 1) to LocalDate(2026, 2, 10)

    private fun plan(n: Int) = FilmPlan(
        List(n) {
            Day(
                LocalDate(2026, 1, 1).plus(it, DateTimeUnit.DAY), "f$it", "t", DaySource.CAMERA,
                takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0),
            )
        },
    )

    @Test fun ranges() {
        assertEquals(LocalDate(2026, 11, 5) to today, rangeFor(FilmRangeType.WEEK, today, custom))
        assertEquals(LocalDate(2026, 11, 1) to LocalDate(2026, 11, 30), rangeFor(FilmRangeType.MONTH, today, custom))
        assertEquals(LocalDate(2026, 1, 1) to LocalDate(2026, 12, 31), rangeFor(FilmRangeType.YEAR, today, custom))
        assertEquals(custom, rangeFor(FilmRangeType.CUSTOM, today, custom))
    }

    @Test fun ticksPerDayUpToLimitThenPerMonth() {
        assertEquals(40, scrubberTicks(plan(40)).size)
        val months = scrubberTicks(plan(90))
        assertEquals(3, months.size)
        assertEquals(0f, months[0])
        assertEquals(31 / 90f, months[1])
    }

    @Test fun dayIndex() {
        assertEquals(0, dayIndexAt(0f, 10))
        assertEquals(9, dayIndexAt(1f, 10))
        assertEquals(5, dayIndexAt(0.55f, 10))
        assertEquals(0, dayIndexAt(0.5f, 0))
    }

    @Test fun fadeOnlyForFadeCut() {
        assertEquals(0f, fadeAlpha(FilmCut.CUT, 0f))
        assertEquals(1f, fadeAlpha(FilmCut.FADE, 0f))
        assertEquals(0f, fadeAlpha(FilmCut.FADE, 0.5f))
        assertEquals(1f, fadeAlpha(FilmCut.FADE, 1f), 0.001f)
    }

    @Test fun optionsBuildFilm() {
        val o = FilmOptions(FilmRangeType.MONTH, custom, FilmAudio.ORIGINAL, "asset:x", FilmCut.FADE, false)
        val f = o.toFilm("id", "Nov", FilmFormat.STORY, today)
        assertEquals(LocalDate(2026, 11, 1), f.rangeFrom)
        assertEquals(FilmCut.FADE, f.cut)
        assertEquals(false, f.dateOn)
        assertNull(f.track)
        assertEquals("asset:x", o.copy(audio = FilmAudio.MIX).toFilm("id", "n", FilmFormat.CINEMA, today).track)
    }
}
