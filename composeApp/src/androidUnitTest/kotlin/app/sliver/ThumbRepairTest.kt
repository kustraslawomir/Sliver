package app.sliver

import app.sliver.feature.trim.videoForThumb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThumbRepairTest {
    @Test
    fun mapsDayThumbToItsVideo() {
        assertEquals("/data/files/days/p-2026-11-09.mp4", videoForThumb("/data/files/thumbs/p-2026-11-09.jpg"))
    }

    @Test
    fun ignoresOtherPaths() {
        assertNull(videoForThumb("/data/cache/tmp/rec_1.mp4.jpg"))
        assertNull(videoForThumb("content://media/1"))
        assertNull(videoForThumb("/data/files/thumbs/p.png"))
    }
}
