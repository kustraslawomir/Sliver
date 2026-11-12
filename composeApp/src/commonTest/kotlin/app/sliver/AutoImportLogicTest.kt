package app.sliver

import app.sliver.feature.autoimport.DraftCandidate
import app.sliver.feature.autoimport.MAX_CANDIDATES
import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.autoimport.candidatesOf
import app.sliver.feature.autoimport.decodeCandidates
import app.sliver.feature.autoimport.draftPositionLabel
import app.sliver.feature.autoimport.draftsChipLabel
import app.sliver.feature.autoimport.emptyDaysLabel
import app.sliver.feature.autoimport.encodeCandidates
import app.sliver.feature.autoimport.gapDays
import app.sliver.feature.autoimport.groupByDate
import app.sliver.feature.autoimport.middleWindowStartMs
import app.sliver.feature.autoimport.nextDraft
import app.sliver.feature.autoimport.previousDraft
import app.sliver.feature.autoimport.rank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AutoImportLogicTest {
    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime(y, m, d, h, min).toInstant(zone).toEpochMilliseconds()

    private fun video(name: String, ms: Long, len: Long) = MediaEntry(name, true, ms, len)
    private fun photo(name: String, ms: Long) = MediaEntry(name, false, ms, 0)
    private fun motion(name: String, ms: Long) = MediaEntry(name, false, ms, 0, motion = true)

    @Test
    fun groupingUsesTheDeviceZoneNotUtc() {
        val tokyo = TimeZone.of("Asia/Tokyo")
        val la = TimeZone.of("America/Los_Angeles")
        // 23:30 UTC on the 10th is the 11th in Tokyo and the 10th in Los Angeles.
        val ms = Instant.parse("2026-11-10T23:30:00Z").toEpochMilliseconds()
        val from = LocalDate(2026, 11, 1)
        val to = LocalDate(2026, 11, 30)
        assertEquals(listOf(LocalDate(2026, 11, 11)), groupByDate(listOf(photo("a", ms)), tokyo, from, to).keys.toList())
        assertEquals(listOf(LocalDate(2026, 11, 10)), groupByDate(listOf(photo("a", ms)), la, from, to).keys.toList())
    }

    @Test
    fun itemsWithoutCaptureTimeOrOutsideTheRangeAreIgnored() {
        val z = TimeZone.UTC
        val items = listOf(photo("none", 0), photo("early", at(z, 2026, 10, 31, 12)), photo("ok", at(z, 2026, 11, 5, 12)), photo("late", at(z, 2026, 11, 20, 12)))
        val grouped = groupByDate(items, z, LocalDate(2026, 11, 1), LocalDate(2026, 11, 12))
        assertEquals(listOf(LocalDate(2026, 11, 5)), grouped.keys.toList())
    }

    @Test
    fun gapsSkipDaysThatHaveARow() {
        val existing = setOf(LocalDate(2026, 11, 2), LocalDate(2026, 11, 4))
        val gaps = gapDays(LocalDate(2026, 11, 1), LocalDate(2026, 11, 5)) { it in existing }
        assertEquals(listOf(1, 3, 5), gaps.map { it.day })
        assertTrue(gapDays(LocalDate(2026, 11, 6), LocalDate(2026, 11, 5)) { false }.isEmpty())
    }

    @Test
    fun longestVideoWins() {
        val e = listOf(video("short", 1, 4_000), video("long", 2, 30_000), photo("p", 3))
        assertEquals("long", rank(e).first().uri)
    }

    @Test
    fun motionPhotoCountsAsAVideoAheadOfPhotos() {
        val e = listOf(photo("p", 1), motion("m", 2))
        assertEquals(listOf("m", "p"), rank(e).map { it.uri })
    }

    @Test
    fun aRealVideoLongerThanTheMotionPhotoBeatsIt() {
        val e = listOf(motion("m", 1), video("v", 2, 8_000), video("tiny", 3, 500))
        assertEquals(listOf("v", "m", "tiny"), rank(e).map { it.uri })
    }

    @Test
    fun photoIsTheDefaultWhenThereIsNoVideo() {
        val e = listOf(photo("b", 20), photo("a", 10))
        assertEquals("a", rank(e).first().uri)
    }

    @Test
    fun candidatesAreCappedAndKeepTheDefaultFirst() {
        val e = List(45) { photo("p$it", it.toLong() + 1) } + video("v", 99, 5_000)
        val c = candidatesOf(e)
        assertEquals(MAX_CANDIDATES, c.size)
        assertEquals("v", c.first().uri)
        assertTrue(c.first().video)
    }

    @Test
    fun candidatesRoundTripThroughJson() {
        val list = listOf(DraftCandidate("content://a", true, 5000), DraftCandidate("content://b", false, 0, motion = true))
        assertEquals(list, decodeCandidates(encodeCandidates(list)))
        assertEquals(emptyList(), decodeCandidates("garbage"))
        assertEquals(emptyList(), decodeCandidates(null))
    }

    @Test
    fun videoWindowSitsInTheMiddle() {
        assertEquals(4_500, middleWindowStartMs(10_000))
        assertEquals(0, middleWindowStartMs(800))
    }

    @Test
    fun labels() {
        assertEquals("1 empty day since 1 Nov 2026", emptyDaysLabel(1, LocalDate(2026, 11, 1)))
        assertEquals("300 empty days since 1 Nov 2026", emptyDaysLabel(300, LocalDate(2026, 11, 1)))
        assertEquals("12 drafts to review", draftsChipLabel(12))
        assertEquals("1 draft to review", draftsChipLabel(1))
        assertEquals("Draft · 3 of 12", draftPositionLabel(2, 12))
    }

    @Test
    fun draftNavigationWraps() {
        val dates = listOf(LocalDate(2026, 11, 1), LocalDate(2026, 11, 5), LocalDate(2026, 11, 9))
        assertEquals(LocalDate(2026, 11, 5), nextDraft(dates, dates[0]) { it })
        assertEquals(LocalDate(2026, 11, 1), nextDraft(dates, dates[2]) { it })
        assertEquals(LocalDate(2026, 11, 9), previousDraft(dates, dates[0]) { it })
        assertEquals(null, nextDraft(listOf(dates[1]), dates[1]) { it })
    }
}
