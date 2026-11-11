package app.sliver

import app.sliver.feature.archive.YearMonth
import app.sliver.feature.backfill.BackfillBatch
import app.sliver.feature.backfill.BackfillItem
import app.sliver.feature.backfill.BackfillRange
import app.sliver.feature.backfill.findCandidates
import app.sliver.feature.backfill.rangeStart
import app.sliver.feature.backfill.selectedCandidates
import app.sliver.feature.backfill.yearRows
import app.sliver.feature.gallery.GalleryItem
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackfillLogicTest {
    private val today = LocalDate(2026, 11, 11)
    private val d1 = LocalDate(2026, 10, 1)
    private val d2 = LocalDate(2026, 10, 2)

    private fun video(uri: String, date: LocalDate, ms: Long) = GalleryItem(uri, true, date, ms, false)
    private fun photo(uri: String, date: LocalDate) = GalleryItem(uri, false, date, 0, false)

    @Test
    fun longestVideoWins() {
        val c = findCandidates(listOf(video("a", d1, 2000), video("b", d1, 9000), photo("p", d1)), emptySet(), today, null, true)
        assertEquals(listOf("b"), c.map { it.uri })
    }

    @Test
    fun photoOnlyWithOptIn() {
        val items = listOf(photo("p1", d1), photo("p2", d1), video("v", d2, 1000))
        assertEquals(listOf("v"), findCandidates(items, emptySet(), today, null, false).map { it.uri })
        assertEquals(listOf("p1", "v"), findCandidates(items, emptySet(), today, null, true).map { it.uri })
    }

    @Test
    fun filledTodayAndFutureAreSkipped() {
        val items = listOf(video("a", d1, 1000), video("t", today, 1000), video("f", LocalDate(2027, 1, 1), 1000))
        assertTrue(findCandidates(items, setOf(d1), today, null, true).isEmpty())
    }

    @Test
    fun rangeBoundsDates() {
        val items = listOf(video("old", LocalDate(2025, 5, 5), 1000), video("new", d1, 1000))
        val from = rangeStart(BackfillRange.THIS_YEAR, LocalDate(2024, 1, 1), today)
        assertEquals(LocalDate(2026, 1, 1), from)
        assertEquals(listOf("new"), findCandidates(items, emptySet(), today, from, true).map { it.uri })
        assertEquals(2, findCandidates(items, emptySet(), today, rangeStart(BackfillRange.EVERYTHING, d1, today), true).size)
    }

    @Test
    fun monthTogglesAndRows() {
        val items = listOf(video("a", LocalDate(2025, 12, 3), 1), video("b", d1, 1), video("c", d2, 1))
        val c = findCandidates(items, emptySet(), today, null, true)
        val rows = yearRows(c)
        assertEquals(listOf(2026, 2025), rows.map { it.year })
        assertEquals(2, rows[0].counts[9])
        assertEquals(1, rows[1].counts[11])
        assertEquals(listOf("a"), selectedCandidates(c, setOf(YearMonth(2026, 10))).map { it.uri })
    }

    @Test
    fun batchProgress() {
        val items = listOf(BackfillItem("2026-10-01", "a", true), BackfillItem("2026-10-02", "b", true))
        val b = BackfillBatch(items)
        assertFalse(b.finished)
        assertEquals(0.5f, b.copy(processed = setOf("2026-10-01")).progress)
        assertTrue(b.copy(processed = setOf("2026-10-01", "2026-10-02")).finished)
    }
}
