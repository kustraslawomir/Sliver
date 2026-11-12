package app.sliver.looks

import app.sliver.data.FilterPresets
import app.sliver.data.candidateNumber
import app.sliver.data.info
import app.sliver.data.look.LookCandidate
import app.sliver.data.look.LutGenerator
import java.io.File
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The candidate pool: `looks/candidates.json` at the repo root (the unit-test working directory is composeApp). */
object Candidates {
    val file = File("../looks/candidates.json")
    val all: List<LookCandidate> by lazy { Json { ignoreUnknownKeys = false }.decodeFromString(file.readText()) }
}

class LookCandidatesTest {
    @Test
    fun poolHasFortyNumberedCandidatesInEightFamilies() {
        val all = Candidates.all
        assertEquals((1..40).toList(), all.map { it.number })
        assertEquals(all.size, all.map { it.name }.toSet().size, "names are unique")
        assertEquals(8, all.map { it.family }.toSet().size)
    }

    @Test
    fun everyCandidateIsAValidParameterSet() {
        for (c in Candidates.all) {
            val m = c.model
            assertTrue(m.bands.isEmpty() || m.bands.size == 6, "${c.name} bands")
            assertTrue(m.gamma.isEmpty() || m.gamma.size == 3, "${c.name} gamma")
            assertTrue(m.mono.isEmpty() || m.mono.size == 3, "${c.name} mono")
            assertTrue(m.shadowTint.isEmpty() || m.shadowTint.size == 3, "${c.name} shadowTint")
            assertTrue(m.highlightTint.isEmpty() || m.highlightTint.size == 3, "${c.name} highlightTint")
            assertTrue(m.grain in 0.0..1.0 && m.vignette in 0.0..1.0 && m.leak in 0.0..1.0 && m.blackLift in 0.0..0.2, c.name)
            val t = LutGenerator.table(m)
            assertTrue(t.all { it in 0f..1f }, "${c.name} LUT in range")
        }
    }

    @Test
    fun filmStockNamesStayOutOfNames() {
        val banned = listOf("kodak", "fuji", "portra", "ektar", "vision", "ilford", "polaroid", "disposable", "tri-x", "velvia", "cinestill", "lomo", "vsco")
        for (c in Candidates.all) banned.forEach { assertTrue(!c.name.lowercase().contains(it), "${c.name} contains $it") }
    }

    @Test
    fun shippedPresetsAreTheirCandidates() {
        for (p in FilterPresets) {
            val number = assertNotNull(p.candidateNumber)
            val c = Candidates.all.first { it.number == number }
            assertEquals(c.model, p.info.model, "${p.name} drifted from candidate ${c.number} ${c.name}")
        }
    }

    @Test
    fun silverUsesTheDocumentedChannelMix() {
        val silver = Candidates.all.first { it.name == "Silver 400" }.model
        assertEquals(listOf(0.5, 0.4, 0.1), silver.mono)
    }
}
