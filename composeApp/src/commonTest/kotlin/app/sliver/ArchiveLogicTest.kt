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
import app.sliver.feature.archive.stepMonth
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
        assertEquals(CellState.DRAFT, cellState(LocalDate(2026, 11, 1), today, day(LocalDate(2026, 11, 1)).copy(draft = true)))
        assertEquals(CellState.AUTO, cellState(LocalDate(2026, 11, 1), today, day(LocalDate(2026, 11, 1)).copy(importRunId = "r")))
        assertEquals(CellState.DRAFT, cellState(LocalDate(2026, 11, 1), today, day(LocalDate(2026, 11, 1)).copy(draft = true, importRunId = "r")))
    }

    @Test
    fun autoCellIsDescribedWithItemCount() {
        val d = LocalDate(2026, 11, 10)
        assertEquals("Tuesday 10 November, auto-filled, 4 items, review", app.sliver.feature.archive.cellDescription(d, true, false, 4))
        assertEquals("Tuesday 10 November, auto-filled, 1 item, review", app.sliver.feature.archive.cellDescription(d, true, false, 1))
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
    fun stepMonthStaysWithinProject() {
        val first = LocalDate(2026, 1, 20)
        val today = LocalDate(2026, 3, 5)
        assertEquals(YearMonth(2026, 2), stepMonth(YearMonth(2026, 3), -1, first, today))
        assertEquals(YearMonth(2026, 1), stepMonth(YearMonth(2026, 1), -1, first, today))
        assertEquals(YearMonth(2026, 3), stepMonth(YearMonth(2026, 3), 1, first, today))
        assertEquals(YearMonth(2026, 3), stepMonth(YearMonth(2026, 1), 5, first, today))
    }

    @Test
    fun header() {
        assertEquals("1 clip · since 3 March", clipsHeader(1, LocalDate(2026, 3, 3)))
        assertEquals("42 clips · since 3 March", clipsHeader(42, LocalDate(2026, 3, 3)))
    }

    @Test
    fun dockCaptionCountsHoursThenMinutesToMidnight() {
        val d = LocalDate(2026, 11, 13)
        assertEquals("Friday 13 Nov · 6 h left", app.sliver.feature.archive.dockCaption(d, 17, 30))
        assertEquals("Friday 13 Nov · 15 min left", app.sliver.feature.archive.dockCaption(d, 23, 45))
    }
}
