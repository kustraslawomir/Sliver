package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.feature.archive.CellState
import app.sliver.feature.archive.MemoryKind
import app.sliver.feature.archive.YearMonth
import app.sliver.feature.archive.canGoNext
import app.sliver.feature.archive.canGoPrev
import app.sliver.feature.archive.cellState
import app.sliver.feature.archive.pickMemory
import app.sliver.feature.archive.clipsHeader
import app.sliver.feature.archive.yearColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class ArchiveLogicTest {
    private val today = LocalDate(2026, 11, 11)

    private fun day(date: LocalDate, source: DaySource = DaySource.CAMERA, color: Int = 0xFF112233.toInt()) =
        Day(date, "f", "t", source, color = color, takenAt = Instant.fromEpochSeconds(0), createdAt = Instant.fromEpochSeconds(0))

    @Test
    fun cellStates() {
        assertEquals(CellState.FUTURE, cellState(LocalDate(2026, 11, 12), today, null))
        assertEquals(CellState.TODAY, cellState(today, today, null))
        assertEquals(CellState.EMPTY, cellState(LocalDate(2026, 11, 1), today, null))
        assertEquals(CellState.FILLED, cellState(today, today, day(today)))
        assertEquals(CellState.BACKFILLED, cellState(today, today, day(today, DaySource.BACKFILL)))
    }

    @Test
    fun monthBounds() {
        val first = LocalDate(2026, 9, 20)
        assertFalse(canGoPrev(YearMonth(2026, 9), first))
        assertTrue(canGoPrev(YearMonth(2026, 10), first))
        assertFalse(canGoNext(YearMonth(2026, 11), today))
        assertTrue(canGoNext(YearMonth(2026, 10), today))
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12).plusMonths(1))
        assertEquals(YearMonth(2025, 12), YearMonth(2026, 1).plusMonths(-1))
    }

    @Test
    fun memoryPicksMostRecentAvailable() {
        val year = LocalDate(2025, 11, 11)
        val hundred = LocalDate(2026, 8, 3)
        val month = LocalDate(2026, 10, 11)
        assertNull(pickMemory(today, emptySet()))
        assertEquals(MemoryKind.YEAR, pickMemory(today, setOf(year))?.kind)
        assertEquals(MemoryKind.HUNDRED, pickMemory(today, setOf(year, hundred))?.kind)
        assertEquals(MemoryKind.MONTH, pickMemory(today, setOf(year, hundred, month))?.kind)
    }

    @Test
    fun yearGrid() {
        val rows = yearColors(2026, mapOf(LocalDate(2026, 2, 3) to day(LocalDate(2026, 2, 3))))
        assertEquals(12, rows.size)
        assertTrue(rows.all { it.size == 31 })
        assertEquals(0xFF112233.toInt(), rows[1][2])
        assertEquals(0, rows[1][0])
        assertNull(rows[1][28])
        assertEquals(0, rows[0][30])
        assertNull(rows[3][30])
    }

    @Test
    fun header() {
        assertEquals("1 clip · since 3 Mar", clipsHeader(1, LocalDate(2026, 3, 3)))
        assertEquals("42 clips · since 3 Mar", clipsHeader(42, LocalDate(2026, 3, 3)))
        assertEquals("2 clips · since 3 Mar · Cinema 16:9", clipsHeader(2, LocalDate(2026, 3, 3), FilmFormat.CINEMA))
    }
}
