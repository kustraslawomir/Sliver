package app.sliver

import app.sliver.data.export.stampAnchor
import app.sliver.data.export.stampTextPx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StampLayoutTest {
    @Test
    fun landscape1080pScalesBy3() {
        assertEquals(36, stampTextPx(1920, 1080))
        val (x, y) = stampAnchor(1920, 1080)
        assertEquals(1f - 96f / 1920, x, 1e-6f)
        assertEquals(-1f + 96f / 1080, y, 1e-6f)
    }

    @Test
    fun anchorIsBottomRightInside() {
        for ((w, h) in listOf(1920 to 1080, 1080 to 1920)) {
            val (x, y) = stampAnchor(w, h)
            assertTrue(x in 0f..1f && y in -1f..0f)
        }
    }

    @Test
    fun marginIsEqualInPixelsOnBothAxes() {
        val (x, y) = stampAnchor(1080, 1920)
        assertEquals((1f - x) * 1080 / 2, (y + 1f) * 1920 / 2, 1e-3f)
    }
}
