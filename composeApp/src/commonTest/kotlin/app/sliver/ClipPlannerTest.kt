package app.sliver

import app.sliver.data.export.ClipPlanner
import app.sliver.data.export.ShortMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ClipPlannerTest {
    @Test fun fullSpeedTakesOneSecond() {
        val p = ClipPlanner.plan(10_000, 2_000, 1f, ShortMode.SLOW_TO_FIT)
        assertEquals(2_000, p.startMs)
        assertEquals(3_000, p.endMs)
    }

    @Test fun halfSpeedTakesHalfSecond() {
        val p = ClipPlanner.plan(10_000, 2_000, 0.5f, ShortMode.SLOW_TO_FIT)
        assertEquals(2_500, p.endMs)
        assertEquals(0.5f, p.speed)
    }

    @Test fun startNearEndShiftsBack() {
        val p = ClipPlanner.plan(5_000, 4_800, 1f, ShortMode.SLOW_TO_FIT)
        assertEquals(4_000, p.startMs)
        assertEquals(5_000, p.endMs)
    }

    @Test fun shortSlowToFit() {
        val p = ClipPlanner.plan(500, 0, 1f, ShortMode.SLOW_TO_FIT)
        assertEquals(0.5f, p.speed)
        assertEquals(500, p.endMs)
    }

    @Test fun shortLoopAndHold() {
        assertEquals(3, ClipPlanner.plan(400, 0, 1f, ShortMode.LOOP).loops)
        assertEquals(600, ClipPlanner.plan(400, 0, 1f, ShortMode.HOLD_LAST).holdMs)
    }

    @Test fun cropPortraitIntoLandscapeUsesOffset() {
        val top = ClipPlanner.cropRect(9f / 16, 16f / 9, null, 0f)
        assertEquals(1f, top.top)
        val center = ClipPlanner.cropRect(9f / 16, 16f / 9, null, null)
        assertEquals(0f, (center.top + center.bottom), 1e-5f)
    }

    @Test fun cropWideIntoNarrowCentersHorizontally() {
        val r = ClipPlanner.cropRect(16f / 9, 9f / 16, null, null)
        assertEquals(-r.left, r.right)
    }

    @Test fun cropWideIntoNarrowUsesHorizontalOffset() {
        val f = (9f / 16) / (16f / 9)
        val left = ClipPlanner.cropRect(16f / 9, 9f / 16, 0f, null)
        assertEquals(-1f, left.left, 1e-5f)
        assertEquals(-1f + 2f * f, left.right, 1e-5f)
        val right = ClipPlanner.cropRect(16f / 9, 9f / 16, 1f, null)
        assertEquals(1f, right.right, 1e-5f)
        assertEquals(1f - 2f * f, right.left, 1e-5f)
        assertEquals(-1f, right.bottom)
        assertEquals(1f, right.top)
    }
}
