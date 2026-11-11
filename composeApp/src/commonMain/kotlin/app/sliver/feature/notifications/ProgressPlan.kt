package app.sliver.feature.notifications

/** How many of [segments] equal segments are complete after [done] of [total] units. */
fun segmentsDone(segments: Int, done: Int, total: Int): Int =
    if (total <= 0 || segments <= 0) 0 else (done.coerceIn(0, total).toLong() * segments / total).toInt()

/** Throttle: true when at least [intervalMs] passed since [lastMs], or nothing was posted yet. */
fun progressDue(lastMs: Long?, nowMs: Long, intervalMs: Long = 1_000) = lastMs == null || nowMs - lastMs >= intervalMs
