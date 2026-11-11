package app.sliver

import app.sliver.feature.notifications.progressDue
import app.sliver.feature.notifications.segmentsDone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressPlanTest {
    @Test fun segmentsFillByProgress() {
        assertEquals(0, segmentsDone(12, 0, 100))
        assertEquals(6, segmentsDone(12, 50, 100))
        assertEquals(11, segmentsDone(12, 99, 100))
        assertEquals(12, segmentsDone(12, 100, 100))
        assertEquals(0, segmentsDone(12, 5, 0))
        assertEquals(12, segmentsDone(12, 500, 100))
    }

    @Test fun throttleAllowsOnePerSecond() {
        assertTrue(progressDue(null, 0))
        assertFalse(progressDue(1000, 1500))
        assertTrue(progressDue(1000, 2000))
    }
}
