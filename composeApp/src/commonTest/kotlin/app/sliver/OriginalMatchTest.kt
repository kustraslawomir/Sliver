package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.db.Day
import app.sliver.data.files.TmpRecording
import app.sliver.feature.trim.RESCUE_MAX_GAP_MS
import app.sliver.feature.trim.matchOriginals
import app.sliver.feature.trim.recordingStamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class OriginalMatchTest {
    private fun day(d: Int, savedAt: Long, source: DaySource = DaySource.CAMERA, original: String? = null) = Day(
        date = LocalDate(2026, 11, d), file = "f$d", thumb = "t$d", source = source, takenAt = Instant.fromEpochMilliseconds(savedAt),
        createdAt = Instant.fromEpochMilliseconds(savedAt), projectId = "p", original = original,
    )

    private fun rec(start: Long, end: Long = start + 3_000) = TmpRecording("tmp/rec_$start.mp4", start, end)

    @Test fun eachDayTakesTheLatestRecordingBeforeItWasSaved() {
        val a = day(12, 100_000)
        val b = day(13, 200_000)
        val got = matchOriginals(listOf(a, b), listOf(rec(50_000), rec(90_000), rec(150_000), rec(190_000)))
        assertEquals(mapOf(b.date to 190_000L, a.date to 90_000L), got.associate { it.first.date to it.second.startMs })
    }

    @Test fun skipsGalleryDaysDaysWithAnOriginalAndRecordingsTooOldOrStillRunning() {
        val saved = RESCUE_MAX_GAP_MS + 100_000
        val got = matchOriginals(
            listOf(day(10, saved, DaySource.GALLERY), day(11, saved, original = "orig_1.mp4"), day(12, saved)),
            listOf(rec(50_000), rec(saved - 1_000, end = saved + 60_000)),
        )
        assertEquals(emptyList(), got)
    }

    @Test fun aRecordingGoesToOneDayOnly() {
        val got = matchOriginals(listOf(day(12, 100_000), day(13, 101_000)), listOf(rec(90_000)))
        assertEquals(listOf(LocalDate(2026, 11, 13)), got.map { it.first.date })
    }

    @Test fun stampFromName() {
        assertEquals(1763000000123, recordingStamp("/x/tmp/rec_1763000000123.mp4"))
        assertEquals(null, recordingStamp("/x/tmp/clip.mp4"))
    }
}
