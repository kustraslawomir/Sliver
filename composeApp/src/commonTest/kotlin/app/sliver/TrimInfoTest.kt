package app.sliver

import app.sliver.feature.trim.clipInfo
import app.sliver.feature.trim.prepareProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class TrimInfoTest {
    @Test fun infoShowsDurationAndSize() {
        assertEquals("4:32 · 812 MB", clipInfo(272_000, 812_000_000))
        assertEquals("0:05 · 1.4 GB", clipInfo(5_000, 1_450_000_000))
        assertEquals("0:05", clipInfo(5_000, 0))
    }

    @Test fun progressGrowsThroughSteps() {
        assertEquals(0f, prepareProgress(false, false, 0, 10))
        assertEquals(0.1f, prepareProgress(true, false, 0, 10))
        assertEquals(0.7f, prepareProgress(true, true, 5, 10), 0.001f)
        assertEquals(1f, prepareProgress(true, true, 10, 10), 0.001f)
    }
}
