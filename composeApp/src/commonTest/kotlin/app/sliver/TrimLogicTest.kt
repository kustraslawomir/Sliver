package app.sliver

import app.sliver.feature.trim.clampStart
import app.sliver.feature.trim.minimapJump
import app.sliver.feature.trim.thumbCount
import app.sliver.feature.trim.tickIndex
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TrimLogicTest {
    @Test fun clampKeepsWindowInsideClip() {
        assertEquals(0, clampStart(-50, 10_000, 1f))
        assertEquals(9_000, clampStart(9_800, 10_000, 1f))
        assertEquals(9_500, clampStart(9_800, 10_000, 0.5f))
    }

    @Test fun ticksEveryQuarterSecond() {
        assertEquals(tickIndex(0), tickIndex(249))
        assertEquals(1, tickIndex(250))
    }

    @Test fun minimapCentersWindow() {
        assertEquals(59_500, minimapJump(0.5f, 120_000, 1f))
        assertEquals(0, minimapJump(0f, 120_000, 1f))
        assertEquals(119_000, minimapJump(1f, 120_000, 1f))
    }

    @Test fun onlyRecordingsAreTmp() {
        assertEquals(true, app.sliver.feature.trim.isTmp("/cache/tmp/rec_1.mp4"))
        assertEquals(false, app.sliver.feature.trim.isTmp("/storage/DCIM/clip.mp4"))
    }

    @Test fun thumbCountCaps() {
        assertEquals(1, thumbCount(500))
        assertEquals(8, thumbCount(7_200))
        assertEquals(120, thumbCount(600_000))
    }
}
