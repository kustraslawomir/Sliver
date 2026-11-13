package app.sliver

import app.sliver.feature.gallery.AddCheck
import app.sliver.feature.gallery.CaptureDate
import app.sliver.feature.gallery.checkCaptureDate
import app.sliver.feature.gallery.parseExifDate
import app.sliver.feature.gallery.parseVideoDate
import app.sliver.feature.gallery.resolveCaptureDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureDateTest {
    private val day = LocalDate(2026, 3, 2)

    @Test
    fun sameDayAllowed() = assertEquals(AddCheck.Allowed, checkCaptureDate(CaptureDate.Known(day), day))

    @Test
    fun otherKnownDayBlockedWithDate() {
        val r = checkCaptureDate(CaptureDate.Known(LocalDate(2026, 2, 28)), day) as AddCheck.Blocked
        assertEquals("This clip was recorded on 28 Feb 2026. It can only be added to that day.", r.message)
    }

    @Test
    fun missingDateIsAllowedWithNotice() =
        assertEquals(AddCheck.AllowedWithNotice("Date unknown — added to 2 Mar 2026"), checkCaptureDate(CaptureDate.Unknown, day))

    @Test
    fun hintIsAllowedOnAnyDayWithNotice() =
        assertEquals(
            AddCheck.AllowedWithNotice("Sent on 10 Nov 2026 — added to 2 Mar 2026"),
            checkCaptureDate(CaptureDate.Hint(LocalDate(2026, 11, 10)), day),
        )

    @Test
    fun cameraDateBeatsFileName() =
        assertEquals(CaptureDate.Known(day), resolveCaptureDate(day, "IMG-20261110-WA0001.jpg"))

    @Test
    fun fileNameIsOnlyAHint() =
        assertEquals(CaptureDate.Hint(LocalDate(2026, 11, 10)), resolveCaptureDate(null, "IMG-20261110-WA0001.jpg"))

    @Test
    fun nothingKnownIsUnknown() = assertEquals(CaptureDate.Unknown, resolveCaptureDate(null, "image.jpg"))

    @Test
    fun videoNearMidnightUsesLocalZone() {
        val raw = "20260301T233000.000Z"
        assertEquals(LocalDate(2026, 3, 1), parseVideoDate(raw, TimeZone.of("UTC")))
        assertEquals(LocalDate(2026, 3, 2), parseVideoDate(raw, TimeZone.of("Europe/Warsaw")))
        assertEquals(LocalDate(2026, 3, 1), parseVideoDate(raw, TimeZone.of("America/New_York")))
    }

    @Test
    fun heicExifDateOriginal() = assertEquals(day, parseExifDate("2026:03:02 00:00:05"))

    @Test
    fun placeholderDatesAreNotCaptureDates() {
        assertEquals(null, parseVideoDate("19040101T000000.000Z", TimeZone.UTC))
        assertEquals(null, parseExifDate("garbage"))
    }
}
