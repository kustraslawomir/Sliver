package app.sliver

import app.sliver.core.time.monthGrid
import app.sliver.core.time.startOfDay
import app.sliver.core.time.toLocalDate
import app.sliver.core.time.weekOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class DateRulesTest {
    @Test fun localDateUsesZone() {
        val i = Instant.parse("2026-11-10T23:30:00Z")
        assertEquals(LocalDate(2026, 11, 10), i.toLocalDate(TimeZone.UTC))
        assertEquals(LocalDate(2026, 11, 11), i.toLocalDate(TimeZone.of("Europe/Warsaw")))
    }

    @Test fun weekIsMondayFirst() {
        val w = weekOf(LocalDate(2026, 11, 8)) // Sunday
        assertEquals(LocalDate(2026, 11, 2), w.first())
        assertEquals(LocalDate(2026, 11, 8), w.last())
        assertEquals(DayOfWeek.MONDAY, w.first().dayOfWeek)
    }

    @Test fun monthStartingOnSunday() {
        val g = monthGrid(2026, 2) // 1 Feb 2026 is Sunday
        assertEquals(6, g.size)
        assertEquals(List(6) { null }, g[0].take(6))
        assertEquals(LocalDate(2026, 2, 1), g[0][6])
        assertEquals(28, g.flatten().count { it != null })
    }

    @Test fun monthStartingOnMondayHasNoLeadingBlanks() {
        val g = monthGrid(2026, 6) // 1 Jun 2026 is Monday
        assertEquals(LocalDate(2026, 6, 1), g[0][0])
        assertEquals(30, g.flatten().count { it != null })
        assertEquals(6, g.size)
    }

    @Test fun dstDaysStayDistinct() {
        val warsaw = TimeZone.of("Europe/Warsaw")
        val start = LocalDate(2026, 3, 29).startOfDay(warsaw) // spring forward, 23h day
        assertEquals(LocalDate(2026, 3, 29), (start + 22.hours).toLocalDate(warsaw))
        assertEquals(LocalDate(2026, 3, 30), (start + 23.hours).toLocalDate(warsaw))
        val fall = LocalDate(2026, 10, 25).startOfDay(warsaw) // 25h day
        assertEquals(LocalDate(2026, 10, 25), (fall + 24.hours).toLocalDate(warsaw))
        assertEquals(LocalDate(2026, 10, 26), (fall + 25.hours).toLocalDate(warsaw))
    }
}
