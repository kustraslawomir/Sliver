package app.sliver

import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.gallery.dayPickerSections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayPickerTest {
    @Test
    fun reelogRecordingsFirstLivePairsOnceDuplicatesGone() {
        val list = listOf(
            MediaEntry("p1", false, 1_000, name = "IMG_1.HEIC"),
            MediaEntry("v1", true, 1_200, durationMs = 2_500, name = "IMG_1.MOV"),
            MediaEntry("v2", true, 50_000, durationMs = 12_000, name = "VID_2.mp4", sizeBytes = 10),
            MediaEntry("v2copy", true, 50_000, durationMs = 12_000, name = "VID_2.mp4", sizeBytes = 10),
            MediaEntry("r1", true, 9_000, durationMs = 9_000, name = "Reelog_2026-10-01_210700.mp4", camera = false),
        )
        val (mine, rest) = dayPickerSections(list, "Reelog")
        assertEquals(listOf("r1"), mine.map { it.entry.uri })
        assertEquals(listOf("p1", "v2"), rest.map { it.entry.uri })
        assertEquals("v1", rest.first().liveVideo)
        assertTrue(rest.first().live)
    }
}
