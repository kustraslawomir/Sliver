package app.sliver.looks

import app.sliver.data.look.LookModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LookSheetsTest {
    // Render the owner sheets: ./gradlew :composeApp:testDebugUnitTest --tests '*LookSheetsTest*' -PlookSheets=1
    // Output goes to docs/qa/looks-film/ (repo root).
    private val out = File("../docs/qa/looks-film")

    @Test
    fun renderSheetsOnRequest() {
        if (System.getProperty("lookSheets") == null) return
        LookSheets.write(LookSheets.tuningSheet(), File(out, "sheet.png"))
        val all = Candidates.all
        LookSheets.write(LookSheets.overview(all), File(out, "overview.png"))
        all.groupBy { it.family }.entries.forEachIndexed { i, (family, items) ->
            LookSheets.write(LookSheets.familySheet(family, items), File(out, "family-${i + 1}-${LookSheets.slug(family)}.png"))
        }
    }

    @Test
    fun aTileRendersAndDiffersPerLook() {
        val frame = LookSheets.portrait()
        val plain = LookSheets.render(frame, LookModel(), "t")
        assertEquals(frame.px.size, plain.size)
        assertTrue(plain.contentEquals(frame.px), "identity look leaves the frame untouched")
        val kiosk = Candidates.all.first { it.number == 1 }.model
        assertTrue(!LookSheets.render(frame, kiosk, "t").contentEquals(frame.px))
        assertTrue(LookSheets.render(frame, kiosk, "t").contentEquals(LookSheets.render(frame, kiosk, "t")), "deterministic")
    }

    @Test
    fun everyCandidateRendersWithoutError() {
        val frame = LookSheets.night(160, 90)
        for (c in Candidates.all) {
            assertEquals(160 * 90, LookSheets.render(frame, c.model, "x-${c.number}", 160, 90).size)
        }
    }
}
