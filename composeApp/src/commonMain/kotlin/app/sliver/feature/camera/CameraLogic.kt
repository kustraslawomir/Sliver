package app.sliver.feature.camera

const val HOLD_THRESHOLD_MS = 300L

val SelfTimerOptions = listOf(0, 3, 10)

fun nextSelfTimer(current: Int): Int =
    SelfTimerOptions[(SelfTimerOptions.indexOf(current) + 1) % SelfTimerOptions.size]

/** mm:ss, e.g. 00:07. */
fun formatTimer(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).coerceAtLeast(0)
    return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
}
