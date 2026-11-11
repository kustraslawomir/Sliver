package app.sliver

import app.sliver.data.export.stampText
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DayShareTest {
    @Test
    fun stampIsDayMonthYearPadded() = assertEquals("05.03.2026", stampText(LocalDate(2026, 3, 5)))
}
