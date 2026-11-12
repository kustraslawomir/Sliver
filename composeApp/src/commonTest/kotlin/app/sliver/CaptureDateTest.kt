package app.sliver

import app.sliver.feature.gallery.AddCheck
import app.sliver.feature.gallery.NO_CAPTURE_DATE
import app.sliver.feature.gallery.checkCaptureDate
import app.sliver.feature.gallery.parseExifDate
import app.sliver.feature.gallery.parseVideoDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureDateTest {
    private val day = LocalDate(2026, 3, 2)

    @Test
    fun sameDayAllowed() = assertEquals(AddCheck.Allowed, checkCaptureDate(day, day))

    @Test
    fun otherDayBlockedWithDate() {
        val r = checkCaptureDate(LocalDate(2026, 2, 28), day) as AddCheck.Blocked
        assertEquals("This clip was recorded on 28 Feb 2026. It can only be added to that day.", r.message)
    }

    @Test
    fun missingDateBlocked() = assertEquals(AddCheck.Blocked(NO_CAPTURE_DATE), checkCaptureDate(null, day))

    @Test
    fun videoNearMidnightUsesLocalZone() {
        val raw = "20260301T233000.000Z"
        assertEquals(LocalDate(2026, 3, 1), parseVideoDate(raw, TimeZone.of("UTC")))
        assertEquals(LocalDate(2026, 3, 2), parseVideoDate(raw, TimeZone.of("Europe/Warsaw")))
        assertEquals(AddCheck.Allowed, checkCaptureDate(parseVideoDate(raw, TimeZone.of("Europe/Warsaw")), day))
        assertEquals(LocalDate(2026, 3, 1), parseVideoDate(raw, TimeZone.of("America/New_York")))
    }

    @Test
    fun motionPhotoUsesImageExifDate() {
        assertEquals(AddCheck.Allowed, checkCaptureDate(parseExifDate("2026:03:02 00:00:05"), day))
        assertEquals(AddCheck.Blocked(NO_CAPTURE_DATE), checkCaptureDate(parseExifDate("garbage"), day))
    }
}

class NotAddedTest {
    private val today = LocalDate(2026, 3, 10)
    private fun item(d: LocalDate) = app.sliver.feature.gallery.GalleryItem("u$d", true, d, 1000)

    @Test
    fun countsUnreadableOutOfRangeAndFilled() {
        val items = listOf(item(LocalDate(2026, 3, 5)), item(LocalDate(2026, 3, 5)), item(LocalDate(2026, 3, 6)), item(LocalDate(2025, 1, 1)), item(today))
        val n = app.sliver.feature.backfill.notAddedCount(6, items, setOf(LocalDate(2026, 3, 6)), today, LocalDate(2026, 3, 1))
        assertEquals(4, n)
        assertEquals("4 not added: no date / outside this project", app.sliver.feature.backfill.notAddedLabel(n))
    }
}
