package app.sliver

import app.sliver.data.backup.clipsLine
import app.sliver.data.backup.driveLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupLinesTest {
    @Test fun clipsLineCountsWithPlurals() {
        assertEquals("No clips yet", clipsLine(0, 0))
        assertEquals("1 clip in Drive", clipsLine(1, 1))
        assertEquals("12 clips in Drive", clipsLine(12, 12))
        assertEquals("3 of 12 clips in Drive", clipsLine(3, 12))
    }

    @Test fun driveLineShowsFreeSpaceAndFlagsLow() {
        val gb = 1L shl 30
        val ok = driveLine(5 * gb, 15 * gb)
        assertFalse(ok.low)
        assertTrue(ok.text.endsWith("free"))
        val low = driveLine(15 * gb + gb / 2, 16 * gb)
        assertTrue(low.low)
        assertTrue(low.text.startsWith("Almost full"))
        assertFalse(driveLine(gb, null).low)
    }
}

class AutoImportPresetsTest {
    @kotlin.test.Test fun presetsDropDuplicatesAndFuture() {
        val today = kotlinx.datetime.LocalDate(2026, 11, 13)
        val p = app.sliver.feature.autoimport.fromPresets(kotlinx.datetime.LocalDate(2026, 1, 1), today)
        kotlin.test.assertEquals(listOf(app.sliver.feature.autoimport.FromPreset.PROJECT_START, app.sliver.feature.autoimport.FromPreset.LAST_30), p.map { it.first })
        kotlin.test.assertEquals("No empty days to fill", app.sliver.feature.autoimport.fillLabel(0))
        kotlin.test.assertEquals("1 empty day to fill", app.sliver.feature.autoimport.fillLabel(1))
    }
}
