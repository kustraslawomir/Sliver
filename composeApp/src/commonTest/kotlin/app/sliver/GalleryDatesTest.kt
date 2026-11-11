package app.sliver

import app.sliver.feature.gallery.parseExifDate
import app.sliver.feature.gallery.parseVideoDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryDatesTest {
    @Test fun exif() = assertEquals(LocalDate(2026, 3, 9), parseExifDate("2026:03:09 14:22:01"))
    @Test fun exifGarbage() = assertNull(parseExifDate("0000:00:00 00:00:00"))
    @Test fun exifNull() = assertNull(parseExifDate(null))
    @Test fun videoUtc() = assertEquals(LocalDate(2026, 3, 9), parseVideoDate("20260309T101500.000Z", TimeZone.UTC))
    @Test fun videoCrossesMidnight() =
        assertEquals(LocalDate(2026, 3, 10), parseVideoDate("20260309T233000.000Z", TimeZone.of("Europe/Warsaw")))
}
