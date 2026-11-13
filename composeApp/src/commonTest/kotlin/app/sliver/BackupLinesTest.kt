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

class ImportStatsTest {
    @kotlin.test.Test fun etaAndThousands() {
        kotlin.test.assertEquals(null, app.sliver.feature.autoimport.etaLabel(2, 10, 1_000, 5_000))
        kotlin.test.assertEquals("About 2 min left", app.sliver.feature.autoimport.etaLabel(10, 20, 0 + 1, 100_001))
        kotlin.test.assertEquals("Less than a minute left", app.sliver.feature.autoimport.etaLabel(10, 11, 1, 10_001))
        kotlin.test.assertEquals("3,412", app.sliver.feature.autoimport.groupThousands(3412))
        kotlin.test.assertEquals("312", app.sliver.feature.autoimport.groupThousands(312))
    }
}

class MediaSourceTest {
    @kotlin.test.Test fun screenshotsAreSkippedAndCameraComesFirst() {
        kotlin.test.assertTrue(app.sliver.feature.autoimport.isScreenCapture("DCIM/Screenshots/", "Screenshot_20261105.jpg"))
        kotlin.test.assertTrue(app.sliver.feature.autoimport.isScreenCapture("DCIM/Screen recordings/", "x.mp4"))
        kotlin.test.assertTrue(app.sliver.feature.autoimport.isScreenCapture("DCIM/ScreenRecorder/", "x.mp4"))
        kotlin.test.assertFalse(app.sliver.feature.autoimport.isScreenCapture("DCIM/Camera/", "20261105_101010.jpg"))
        kotlin.test.assertTrue(app.sliver.feature.autoimport.isCameraPath("DCIM/Camera/"))
        kotlin.test.assertFalse(app.sliver.feature.autoimport.isCameraPath("Pictures/Telegram/"))
        kotlin.test.assertFalse(app.sliver.feature.autoimport.isCameraPath("Download/"))
        val tg = app.sliver.feature.autoimport.MediaEntry("tg", video = true, takenAtMs = 1, durationMs = 60_000, camera = false)
        val cam = app.sliver.feature.autoimport.MediaEntry("cam", video = false, takenAtMs = 2, camera = true)
        kotlin.test.assertEquals(listOf("cam", "tg"), app.sliver.feature.autoimport.rank(listOf(tg, cam)).map { it.uri })
    }
}
