package app.sliver

import app.sliver.data.backup.driveRoomLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DriveRoomTest {
    @Test
    fun almostFullSaysWhatItMeans() {
        val limit = 15_000_000_000L
        assertEquals("465 MB free · about 232 more clips", driveRoomLine(limit - 465_000_000L, limit))
        assertNull(driveRoomLine(1_000_000L, limit))
        assertNull(driveRoomLine(1_000_000L, null))
    }
}
