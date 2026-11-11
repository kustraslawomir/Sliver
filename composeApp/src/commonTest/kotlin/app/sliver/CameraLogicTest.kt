package app.sliver

import app.sliver.feature.camera.displaySize
import app.sliver.feature.camera.formatTimer
import app.sliver.feature.camera.surfaceRotation
import app.sliver.feature.camera.nextSelfTimer
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraLogicTest {
    @Test
    fun selfTimerCyclesOff3_10() {
        assertEquals(3, nextSelfTimer(0))
        assertEquals(10, nextSelfTimer(3))
        assertEquals(0, nextSelfTimer(10))
    }

    @Test
    fun timerFormatsMinutesAndSeconds() {
        assertEquals("00:00", formatTimer(0))
        assertEquals("00:07", formatTimer(7_999))
        assertEquals("02:05", formatTimer(125_000))
        assertEquals("00:00", formatTimer(-5))
    }

    @Test
    fun surfaceRotationMapsOrientationDegrees() {
        assertEquals(0, surfaceRotation(0))
        assertEquals(0, surfaceRotation(350))
        assertEquals(3, surfaceRotation(90))
        assertEquals(2, surfaceRotation(180))
        assertEquals(1, surfaceRotation(270))
        assertEquals(1, surfaceRotation(315 - 1))
        assertEquals(0, surfaceRotation(315))
    }

    @Test
    fun displaySizeSwapsForSidewaysMetadata() {
        assertEquals(1920 to 1080, displaySize(1920, 1080, 0))
        assertEquals(1080 to 1920, displaySize(1920, 1080, 90))
        assertEquals(1920 to 1080, displaySize(1080, 1920, 270))
        assertEquals(1920 to 1080, displaySize(1920, 1080, 180))
    }
}
