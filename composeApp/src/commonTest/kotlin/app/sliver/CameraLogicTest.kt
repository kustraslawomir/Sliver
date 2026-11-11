package app.sliver

import app.sliver.feature.camera.formatTimer
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
    fun todayCardDetailFormatsLookAndTime() {
        val at = kotlin.time.Instant.parse("2026-11-11T09:05:00Z")
        assertEquals(
            "Night Neon · 09:05",
            app.sliver.feature.camera.todayCardDetail(app.sliver.data.LookPreset.NIGHT_NEON, at, kotlinx.datetime.TimeZone.UTC),
        )
    }

    @Test
    fun timerFormatsMinutesAndSeconds() {
        assertEquals("00:00", formatTimer(0))
        assertEquals("00:07", formatTimer(7_999))
        assertEquals("02:05", formatTimer(125_000))
        assertEquals("00:00", formatTimer(-5))
    }
}
