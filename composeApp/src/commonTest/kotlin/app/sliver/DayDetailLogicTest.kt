package app.sliver

import app.sliver.feature.archive.dayNumber
import app.sliver.feature.archive.nextDay
import app.sliver.feature.archive.prevDay
import app.sliver.feature.archive.weekdayName
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DayDetailLogicTest {
    private fun d(day: Int) = LocalDate(2026, 3, day)

    @Test fun neighboursSkipGaps() {
        val filled = listOf(d(2), d(9), d(20))
        assertEquals(d(2), prevDay(d(9), filled))
        assertEquals(d(20), nextDay(d(9), filled))
        assertNull(prevDay(d(2), filled))
        assertNull(nextDay(d(20), filled))
    }

    @Test fun numbering() {
        assertEquals(1, dayNumber(d(1), d(1)))
        assertEquals(10, dayNumber(d(10), d(1)))
        assertEquals("Tuesday", weekdayName(LocalDate(2026, 3, 3)))
    }
}
