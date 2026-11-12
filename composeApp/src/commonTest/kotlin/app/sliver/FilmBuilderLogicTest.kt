package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmAudio
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.db.Day
import app.sliver.feature.films.FilmOptions
import app.sliver.feature.films.FilmPlan
import app.sliver.feature.films.dayIndexAt
import app.sliver.feature.films.canSaveSound
import app.sliver.feature.films.isLegacyTrack
import app.sliver.feature.films.trackLabel
import app.sliver.feature.films.usableTrack
import app.sliver.feature.films.rangeFor
import app.sliver.feature.films.scrubberTicks
import app.sliver.feature.films.toFilm
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test fun optionsBuildFilm() {
        val o = FilmOptions(FilmRangeType.MONTH, custom, FilmAudio.ORIGINAL, "/music/1_x.mp3", false)
        val f = o.toFilm("id", "Nov", FilmFormat.STORY, today)
        assertEquals(LocalDate(2026, 11, 1), f.rangeFrom)
        assertEquals(false, f.dateOn)
        assertNull(f.track)
        assertEquals("/music/1_x.mp3", o.copy(audio = FilmAudio.MIX).toFilm("id", "n", FilmFormat.CINEMA, today).track)
    }

    @Test fun legacyAssetTrackIsDropped() {
        assertTrue(isLegacyTrack("asset:music/drift.aac"))
        assertNull(usableTrack("asset:music/drift.aac"))
        assertNull(trackLabel("asset:music/drift.aac"))
        assertFalse(canSaveSound(FilmAudio.MUSIC, "asset:music/drift.aac"))
        assertNull(FilmOptions(custom = custom, audio = FilmAudio.MUSIC, track = "asset:music/drift.aac").toFilm("id", "n", FilmFormat.CINEMA, today).track)
    }

    @Test fun musicNeedsPickedFile() {
        assertTrue(canSaveSound(FilmAudio.ORIGINAL, null))
        assertFalse(canSaveSound(FilmAudio.MUSIC, null))
        assertFalse(canSaveSound(FilmAudio.MIX, null))
        assertTrue(canSaveSound(FilmAudio.MIX, "/files/music/17_song.mp3"))
        assertEquals("song.mp3", trackLabel("/files/music/17_song.mp3"))
    }
}
