package app.sliver.data.export

import kotlin.math.abs

/** Start (ms) of the 1 s window with the highest summed luminance change; ties go to the window nearest the middle. */
fun bestClip(frames: List<FloatArray>, fps: Int): Long {
    val diffs = frames.zipWithNext { a, b ->
        var sum = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) sum += abs(a[i] - b[i])
        if (n == 0) 0f else sum / n
    }
    val window = fps.coerceAtLeast(1)
    if (diffs.size <= window) return 0L
    val starts = diffs.size - window
    var sum = diffs.take(window).sum()
    var best = 0
    var bestSum = sum
    val mid = starts / 2.0
    for (s in 1..starts) {
        sum += diffs[s + window - 1] - diffs[s - 1]
        if (sum > bestSum + EPS || (abs(sum - bestSum) <= EPS && abs(s - mid) < abs(best - mid))) {
            best = s
            bestSum = sum
        }
    }
    return best * SECOND_MS / window
}

private const val EPS = 1e-3f
