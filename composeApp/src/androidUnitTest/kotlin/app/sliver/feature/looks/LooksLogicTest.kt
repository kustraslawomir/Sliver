package app.sliver.feature.looks

import app.sliver.data.LookPreset
import app.sliver.data.toParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LooksLogicTest {
    @Test
    fun setClampsAndResetReturnsPresetDefault() {
        val p = LookPreset.NOIR.toParams().with(AdjustTool.GRAIN, 250)
        assertEquals(100, p.grain)
        assertTrue(p.isModified(AdjustTool.GRAIN))
        assertEquals(35, p.reset(AdjustTool.GRAIN).grain)
        assertEquals(-100, p.with(AdjustTool.CONTRAST, -400).contrast)
    }

    @Test
    fun resetAllKeepsPresetAndIntensity() {
        val p = LookPreset.VHS_94.toParams(60).with(AdjustTool.SATURATION, 40).with(AdjustTool.FADE, 0)
        val r = p.resetAll()
        assertEquals(LookPreset.VHS_94, r.preset)
        assertEquals(60, r.intensity)
        assertEquals(0, r.saturation)
        assertEquals(35, r.fade)
        assertFalse(r.isModified(AdjustTool.FADE))
    }

    @Test
    fun fractionRoundTrips() {
        for (t in AdjustTool.entries) for (v in listOf(0, 37, 100)) assertEquals(v, t.fromFraction(t.toFraction(v)))
        assertEquals(0.5f, AdjustTool.CONTRAST.toFraction(0))
    }

    @Test
    fun applyAndResumeAreConsumedOnce() {
        LookSession.applied = LookPreset.EMBER.toParams()
        assertEquals(LookPreset.EMBER, LookSession.takeApplied()?.preset)
        assertEquals(null, LookSession.takeApplied())
    }
}
