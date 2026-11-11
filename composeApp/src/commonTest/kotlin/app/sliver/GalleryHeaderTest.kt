package app.sliver

import app.sliver.feature.gallery.dayHeader
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryHeaderTest {
    @Test fun headerIsCapsWeekdayDayMonth() {
        assertEquals("MON 9 NOV", dayHeader(LocalDate(2026, 11, 9)))
    }
}
