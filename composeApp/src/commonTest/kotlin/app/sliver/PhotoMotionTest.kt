package app.sliver

import app.sliver.data.export.PhotoMotion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoMotionTest {
    @Test
    fun zoomRunsFrom100To108() {
        assertEquals(1f, PhotoMotion.zoomScale(0f))
        assertEquals(1.08f, PhotoMotion.zoomScale(1f), 1e-6f)
        assertEquals(1.04f, PhotoMotion.zoomScale(0.5f), 1e-6f)
    }

    @Test
    fun zoomIsMonotonicAndClamped() {
        var prev = 0f
        for (i in 0..30) {
            val s = PhotoMotion.zoomScale(i / 30f)
            assertTrue(s >= prev)
            prev = s
        }
        assertEquals(1.08f, PhotoMotion.zoomScale(2f), 1e-6f)
    }
}
