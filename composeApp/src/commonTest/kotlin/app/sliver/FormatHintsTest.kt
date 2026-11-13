package app.sliver

import app.sliver.feature.camera.cinemaBandFraction
import app.sliver.feature.camera.cropXFromLeft
import app.sliver.feature.camera.cropYFromTop
import app.sliver.feature.camera.leftFromCropX
import app.sliver.feature.camera.storyBandFraction
import app.sliver.feature.camera.iconRotation
import app.sliver.feature.camera.isUpright
import app.sliver.feature.camera.topFromCropY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatHintsTest {
    @Test
    fun uprightAndIconRotation() {
        assertTrue(isUpright(0))
        assertTrue(isUpright(350))
        assertTrue(isUpright(180))
        assertFalse(isUpright(90))
        assertFalse(isUpright(270))
        assertEquals(-90f, iconRotation(80))
        assertEquals(0f, iconRotation(355))
    }

    @Test
    fun bandAndCropYRoundTrip() {
        val band = cinemaBandFraction(900f, 1600f)
        assertEquals(0.3164f, band, 0.001f)
        assertEquals(0.5f, cropYFromTop(topFromCropY(0.5f, band), band), 0.0001f)
        assertEquals(0f, cropYFromTop(0f, band))
        assertEquals(1f, cropYFromTop(1f - band, band), 0.0001f)
    }

    @Test
    fun storyBandAndCropXRoundTrip() {
        val band = storyBandFraction(1920f, 1080f)
        assertEquals(0.3164f, band, 0.001f)
        assertEquals(1f, storyBandFraction(500f, 1600f))
        assertEquals(0.5f, cropXFromLeft(leftFromCropX(0.5f, band), band), 0.0001f)
        assertEquals(0f, cropXFromLeft(0f, band))
        assertEquals(1f, cropXFromLeft(1f - band, band), 0.0001f)
        assertEquals(0.5f, cropXFromLeft(0.3f, 1f))
    }
}
