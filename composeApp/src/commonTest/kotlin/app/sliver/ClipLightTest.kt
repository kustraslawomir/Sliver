package app.sliver

import app.sliver.data.light.clipLight
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipLightTest {
    private fun solid(argb: Int) = IntArray(32 * 18) { argb }

    @Test fun noFramesMeansNoLight() = assertNull(clipLight(emptyList()))

    @Test fun whiteIsFullyBrightAndBlackIsDark() {
        assertEquals(1f, clipLight(listOf(solid(0xFFFFFFFF.toInt())))!!.luma, 0.001f)
        assertEquals(0f, clipLight(listOf(solid(0xFF000000.toInt())))!!.luma, 0.001f)
    }

    @Test fun lumaFollowsRec709() {
        assertEquals(0.2126f, clipLight(listOf(solid(0xFFFF0000.toInt())))!!.luma, 0.002f)
        assertEquals(0.7152f, clipLight(listOf(solid(0xFF00FF00.toInt())))!!.luma, 0.002f)
    }

    @Test fun toneIsOpaqueAndKeepsTheHue() {
        val orange = clipLight(listOf(solid(0xFFC98E6E.toInt())))!!.tone
        assertEquals(0xFF, orange ushr 24)
        assertTrue(abs((orange shr 16 and 0xFF) - 0xC9) <= 1 && abs((orange and 0xFF) - 0x6E) <= 1)
    }

    @Test fun saturatedPixelsOutweighGreyOnes() {
        val frame = IntArray(32 * 18) { if (it < 32 * 9) 0xFF808080.toInt() else 0xFFFF0000.toInt() }
        val tone = clipLight(listOf(frame))!!.tone
        assertTrue((tone shr 16 and 0xFF) > (tone and 0xFF) + 60)
    }

    @Test fun threeFramesAreAveraged() {
        val l = clipLight(listOf(solid(0xFF000000.toInt()), solid(0xFFFFFFFF.toInt()), solid(0xFF000000.toInt())))!!.luma
        assertEquals(1f / 3f, l, 0.01f)
    }
}
