package app.sliver

import app.sliver.data.export.SamplingPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SamplingPlanTest {
    @Test
    fun shortClipsAreScoredDensely() {
        assertFalse(SamplingPlan.isCoarse(10_000))
        assertTrue(SamplingPlan.isCoarse(10_001))
    }

    @Test
    fun coarsePassUsesTwoFpsUntilCapped() {
        assertEquals(500, SamplingPlan.coarseStepMs(30_000))
        assertEquals(500, SamplingPlan.coarseStepMs(75_000))
    }

    @Test
    fun coarsePassNeverExceedsFrameCap() {
        for (d in listOf(120_000L, 600_000L, 3_600_000L)) {
            val frames = d / SamplingPlan.coarseStepMs(d) + 1
            assertTrue(frames <= SamplingPlan.MAX_COARSE_FRAMES + 1, "$d -> $frames")
        }
    }

    @Test
    fun refineRangeIsClampedToTheClip() {
        assertEquals(0L..2_000L, SamplingPlan.refineRange(0, 30_000))
        assertEquals(28_500L..30_000L, SamplingPlan.refineRange(29_500, 30_000))
        assertEquals(0L, SamplingPlan.refineRange(200, 30_000).first)
    }

    @Test
    fun fallbackIsTheMiddleSecond() {
        assertEquals(14_500L, SamplingPlan.fallbackStartMs(30_000))
        assertEquals(0L, SamplingPlan.fallbackStartMs(800))
    }
}
