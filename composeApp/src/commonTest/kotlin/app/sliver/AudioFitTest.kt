package app.sliver

import app.sliver.data.export.AudioFit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioFitTest {
    @Test fun keepsOnlyWholeFramesWithinOneSecond() {
        val rate = 44_100
        val frame = AudioFit.frameUs(rate)
        val kept = (0 until 50).count { AudioFit.keep(it * frame, rate) }
        assertEquals(43, kept)
        assertTrue(kept * frame <= 1_000_000)
        assertTrue(1_000_000 - kept * frame < frame)
    }

    @Test fun rejectsFrameThatCrossesTheLimit() {
        assertTrue(!AudioFit.keep(990_000, 48_000))
    }
}
