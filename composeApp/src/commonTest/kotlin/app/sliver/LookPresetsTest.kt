package app.sliver

import app.sliver.data.LookPreset
import app.sliver.data.info
import app.sliver.data.toParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LookPresetsTest {
    @Test
    fun naturalIsIdentity() {
        assertNull(LookPreset.NATURAL.info.lutFile)
        assertEquals(0, LookPreset.NATURAL.toParams().grain)
    }

    @Test
    fun allOtherPresetsHaveLut() {
        LookPreset.entries.filter { it != LookPreset.NATURAL }.forEach {
            assertEquals(true, it.info.lutFile?.endsWith(".cube"))
        }
    }

    @Test
    fun vhsDefaults() {
        val p = LookPreset.VHS_94.toParams()
        assertEquals(35, p.fade)
        assertEquals(55, p.grain)
    }
}
