package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.db.Day
import app.sliver.feature.archive.barIndexAt
import app.sliver.feature.archive.brightestOfMonth
import app.sliver.feature.archive.lightOf
import app.sliver.feature.archive.nearestClipDay
import app.sliver.feature.archive.toneOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class LightOfYearTest {
    private fun day(date: LocalDate, luma: Float = -1f, tone: Int = 0, color: Int = 0) =
        Day(date, "f", "t", DaySource.CAMERA, color = color, takenAt = Instant.fromEpochSeconds(0), createdAt = Instant.fromEpochSeconds(0), luma = luma, tone = tone)

    private val d1 = LocalDate(2026, 10, 1)

    @Test fun storedLumaWinsAndOldClipsFallBackToTheirColour() {
        assertEquals(0.4f, lightOf(day(d1, luma = 0.4f, color = 0xFFFFFFFF.toInt())))
        assertEquals(1f, lightOf(day(d1, color = 0xFFFFFFFF.toInt())), 0.01f)
        assertEquals(0xFF123456.toInt(), toneOf(day(d1, tone = 0xFF123456.toInt(), color = 5)))
        assertEquals(5, toneOf(day(d1, color = 5)))
    }

    @Test fun tapMapsToTheBarUnderTheFinger() {
        assertEquals(0, barIndexAt(0f, 350f, 100))
        assertEquals(50, barIndexAt(175f, 350f, 100))
        assertEquals(99, barIndexAt(999f, 350f, 100))
    }

    @Test fun nearestClipDayLooksTwoBarsEitherSideAndPrefersTheExactOne() {
        val clips = setOf(LocalDate(2026, 10, 4), LocalDate(2026, 10, 9))
        assertEquals(LocalDate(2026, 10, 4), nearestClipDay(3, d1, 2) { it in clips })
        assertEquals(LocalDate(2026, 10, 4), nearestClipDay(5, d1, 2) { it in clips })
        assertNull(nearestClipDay(6, d1, 1) { it in clips && it.day == 99 })
    }

    @Test fun brightestOfMonthIgnoresOtherMonths() {
        val days = listOf(
            day(LocalDate(2026, 10, 3), luma = 0.3f), day(LocalDate(2026, 10, 20), luma = 0.8f),
            day(LocalDate(2026, 11, 2), luma = 0.99f),
        )
        assertEquals(LocalDate(2026, 10, 20), brightestOfMonth(days, LocalDate(2026, 10, 3)))
        assertEquals(LocalDate(2026, 11, 2), brightestOfMonth(days, LocalDate(2026, 11, 30)))
    }
}
