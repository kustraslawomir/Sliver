package app.sliver

import app.sliver.feature.gallery.CaptureDate
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.filenameDateHint
import app.sliver.feature.gallery.groupLivePhotos
import app.sliver.feature.gallery.parseExifDate
import app.sliver.feature.gallery.parseExifMillis
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

    private val nov10 = LocalDate(2026, 11, 10)

    @Test fun telegramPhoto() = assertEquals(nov10, filenameDateHint("photo_2026-11-10_14-22-31.jpg"))
    @Test fun telegramVideo() = assertEquals(nov10, filenameDateHint("video_2026-11-10_14-22-31.mp4"))
    @Test fun whatsappImage() = assertEquals(nov10, filenameDateHint("IMG-20261110-WA0001.jpg"))
    @Test fun whatsappVideo() = assertEquals(nov10, filenameDateHint("VID-20261110-WA0001.mp4"))
    @Test fun signal() = assertEquals(nov10, filenameDateHint("signal-2026-11-10-142231.jpg"))
    @Test fun genericImg() = assertEquals(nov10, filenameDateHint("IMG_20261110_142231.jpg"))
    @Test fun pixel() = assertEquals(nov10, filenameDateHint("PXL_20261110_142231.mp4"))
    @Test fun screenshot() = assertEquals(nov10, filenameDateHint("Screenshot_20261110-142231.png"))
    @Test fun noDate() = assertNull(filenameDateHint("IMG_1234.HEIC"))
    @Test fun longDigitRunIsNotADate() = assertNull(filenameDateHint("1720261110123456.jpg"))
    @Test fun impossibleDate() = assertNull(filenameDateHint("IMG_20261340_142231.jpg"))
    @Test fun nullName() = assertNull(filenameDateHint(null))

    @Test fun exifMillisUsesLocalClock() {
        val utc = parseExifMillis("2026:03:09 12:00:00", TimeZone.UTC)!!
        assertEquals(utc - 3_600_000, parseExifMillis("2026:03:09 12:00:00", TimeZone.of("Europe/Warsaw")))
    }

    private fun item(name: String, video: Boolean, taken: Long? = null, duration: Long = 0) =
        GalleryItem("content://$name", video, CaptureDate.Unknown, duration, name, taken)

    @Test fun livePhotoPairsByBaseName() {
        val g = groupLivePhotos(listOf(item("IMG_0042.HEIC", false), item("img_0042.MOV", true, duration = 3000)))
        assertEquals(1, g.size)
        assertEquals("content://img_0042.MOV", g[0].liveVideo?.uri)
    }

    @Test fun livePhotoPairsByCaptureTime() {
        assertEquals(1, groupLivePhotos(listOf(item("a.jpg", false, 10_000), item("b.mov", true, 11_500, 3000))).size)
    }

    @Test fun farApartDoesNotPair() {
        assertEquals(2, groupLivePhotos(listOf(item("a.jpg", false, 10_000), item("b.mov", true, 13_000, 3000))).size)
    }

    @Test fun longVideoWithSameNameDoesNotPair() {
        assertEquals(2, groupLivePhotos(listOf(item("a.jpg", false), item("a.mov", true, duration = 60_000))).size)
    }

    @Test fun stillAloneIsOneGroup() {
        val g = groupLivePhotos(listOf(item("a.heic", false)))
        assertEquals(1, g.size)
        assertNull(g[0].liveVideo)
    }
}
