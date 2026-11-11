package app.sliver.data.export

/** Which frames to score: dense for short clips, coarse then refined for long ones, with a hard frame cap. */
object SamplingPlan {
    const val FINE_FPS = 10
    const val COARSE_FPS = 2
    const val SHORT_CLIP_MS = 10_000L
    const val MAX_COARSE_FRAMES = 150
    const val REFINE_MS = 1_000L
    const val TIMEOUT_MS = 8_000L

    fun isCoarse(durationMs: Long) = durationMs > SHORT_CLIP_MS

    /** Spacing of the coarse pass; stretched so a very long clip never exceeds [MAX_COARSE_FRAMES]. */
    fun coarseStepMs(durationMs: Long): Long {
        val base = 1000L / COARSE_FPS
        val capped = (durationMs + MAX_COARSE_FRAMES - 1) / MAX_COARSE_FRAMES
        return maxOf(base, capped)
    }

    /** Window size in diffs for a coarse pass with [stepMs] spacing. */
    fun coarseWindowFps(stepMs: Long) = (1000L / stepMs).toInt().coerceAtLeast(1)

    fun refineRange(coarseStartMs: Long, durationMs: Long): LongRange {
        val from = (coarseStartMs - REFINE_MS).coerceAtLeast(0)
        val to = (coarseStartMs + SECOND_MS + REFINE_MS).coerceAtMost(durationMs)
        return from..to
    }

    fun fallbackStartMs(durationMs: Long) = (durationMs / 2 - SECOND_MS / 2).coerceIn(0, (durationMs - SECOND_MS).coerceAtLeast(0))
}
