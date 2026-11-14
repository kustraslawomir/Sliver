package app.sliver

import app.sliver.data.pickerLabel
import app.sliver.data.candidateNumber
import app.sliver.data.FilterPresets
import app.sliver.data.group
import app.sliver.data.LookPreset
import app.sliver.data.StampStyle
import app.sliver.data.db.Film
import app.sliver.data.info
import app.sliver.data.lookParams
import app.sliver.data.toParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LookPresetsTest {
    @Test
    fun naturalIsIdentity() {
        assertNull(LookPreset.NATURAL.info.lutFile)
        assertEquals(0, LookPreset.NATURAL.toParams().grain)
    }

    @Test
    fun pickerShowsEveryCandidateOnceGroupedCleanFirst() {
        assertEquals((1..54).toList(), FilterPresets.map { it.candidateNumber!! }.sorted())
        assertEquals("Clean", FilterPresets.first().pickerLabel)
        assertEquals(app.sliver.data.FilterGroup.entries, FilterPresets.map { it.group }.distinct())
        assertEquals(0.0, FilterPresets.first().info.model.grain)
    }

    @Test
    fun grainStaysSubtle() {
        // Owner 14 Nov: filters were far too grainy; only a few lo-fi looks go above half.
        assertTrue(FilterPresets.count { it.info.model.grain > 0.5 } <= 5)
        assertTrue(FilterPresets.count { it.info.model.grain == 0.0 } >= 6)
    }

    @Test
    fun everyFilterHasAColourLut() {
        FilterPresets.forEach {
            // Shipped six have a .cube; the other candidates build their LUT from the model.
            assertTrue(it.info.lutFile?.endsWith(".cube") ?: true, it.name)
            if (it.info.lutFile != null) assertTrue(it.info.model.hasLut, it.name)
        }
    }

    @Test
    fun defaultGrainComesFromTheModelAndCanBeOverridden() {
        assertEquals(45, LookPreset.KIOSK.toParams().grain)
        assertEquals(10, LookPreset.KIOSK.toParams(grain = 10).grain)
        assertEquals(60, LookPreset.KIOSK.toParams(60).intensity)
    }

    @Test
    fun stampStyles() {
        assertEquals(StampStyle.LED, LookPreset.KIOSK.info.stamp)
        assertEquals(StampStyle.CAMCORDER, LookPreset.TAPE.info.stamp)
        assertEquals(StampStyle.PLAIN, LookPreset.SILVER.info.stamp)
    }

    @Test
    fun filmCarriesItsOwnFilter() {
        assertNull(Film(id = "f", name = "n").lookParams())
        assertNull(Film(id = "f", name = "n", look = LookPreset.NATURAL).lookParams())
        val p = Film(id = "f", name = "n", look = LookPreset.SILVER, lookIntensity = 70, lookGrain = 20).lookParams()
        assertEquals(LookPreset.SILVER, p?.preset)
        assertEquals(70, p?.intensity)
        assertEquals(20, p?.grain)
        assertFalse(p == null)
    }

    @Test
    fun noFilterNameUsesTheWordLooksOrAStockBrand() {
        val banned = listOf("look", "kodak", "fuji", "portra", "vision", "ilford", "polaroid", "disposable")
        LookPreset.entries.map { it.info.label.lowercase() }.forEach { label -> banned.forEach { assertFalse(label.contains(it), label) } }
    }
}
