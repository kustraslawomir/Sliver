package app.sliver

import app.sliver.data.export.stampText
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StampTest {
    @Test
    fun stampIsDayShortMonthYear() {
        assertEquals("5 MAR 2026", stampText(LocalDate(2026, 3, 5)))
        assertEquals("31 OCT 2026", stampText(LocalDate(2026, 10, 31)))
    }
}
