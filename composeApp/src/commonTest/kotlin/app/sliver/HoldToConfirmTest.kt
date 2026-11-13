package app.sliver

import app.sliver.core.ui.holdComplete
import app.sliver.core.ui.holdProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HoldToConfirmTest {
    @Test fun progressGrowsAndClamps() {
        assertEquals(0f, holdProgress(0))
        assertEquals(0.5f, holdProgress(600, 1200))
        assertEquals(1f, holdProgress(5000, 1200))
    }

    @Test fun completesOnlyAtTheDuration() {
        assertFalse(holdComplete(1199, 1200))
        assertTrue(holdComplete(1200, 1200))
    }
}
