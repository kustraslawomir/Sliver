package app.sliver

import app.sliver.feature.camera.displaySize
import app.sliver.feature.camera.focusFraction
import app.sliver.feature.camera.focusRingCenter
import app.sliver.feature.camera.clipLongEnough
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
        assertEquals("0:00", formatTimer(0))
        assertEquals("0:07", formatTimer(7_999))
        assertEquals("2:05", formatTimer(125_000))
        assertEquals("0:00", formatTimer(-5))
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

    @Test
    fun focusPointIsNormalisedAndClamped() {
        assertEquals(0.25f to 0.5f, focusFraction(100f, 200f, 400f, 400f))
        assertEquals(1f to 0f, focusFraction(900f, -10f, 400f, 400f))
        assertEquals(0.5f to 0.5f, focusFraction(10f, 10f, 0f, 0f))
    }

    @Test
    fun focusRingStaysInsideViewfinder() {
        assertEquals(44f to 44f, focusRingCenter(2f, 1f, 400f, 800f, 88f))
        assertEquals(356f to 756f, focusRingCenter(399f, 799f, 400f, 800f, 88f))
        assertEquals(200f to 300f, focusRingCenter(200f, 300f, 400f, 800f, 88f))
    }

    @Test
    fun clipIsLongEnoughAtOneSecond() {
        assertEquals(false, clipLongEnough(999))
        assertEquals(true, clipLongEnough(1000))
    }
}
