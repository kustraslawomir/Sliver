package app.sliver

import app.sliver.feature.archive.cellDescription
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CellDescriptionTest {
    private val date = LocalDate(2026, 11, 10)

    @Test
    fun withSecond() = assertEquals("Tuesday 10 November, has a clip", cellDescription(date, true))

    @Test
    fun withoutSecond() = assertEquals("Tuesday 10 November", cellDescription(date, false))

    @Test
    fun draft() = assertEquals("Tuesday 10 November, draft, review", cellDescription(date, false, draft = true))
}
