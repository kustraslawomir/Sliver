package app.sliver.data.look

import kotlin.math.PI
import kotlin.math.sin

/** Light leak for one clip: where it sits (uv), how big and which orange/red mix; [strength] 0 when the clip has none. */
data class LeakState(val strength: Float, val x: Float, val y: Float, val radius: Float, val r: Float, val g: Float, val b: Float)

/** Per-frame jitter: weave offset in px (+-0.5 at full amount) and an exposure multiplier (+-1.5 % at full amount). */
data class FrameJitter(val dx: Float, val dy: Float, val exposure: Float)

/**
 * Everything random in a look is a pure function of (clip id, frame time), so the preview and the export of the same
 * clip draw the same grain seed, weave, flicker and light leak.
 */
object LookRandom {
    const val FPS = 24

    fun seedOf(clipId: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (ch in clipId) {
            h = h xor ch.code.toLong()
            h *= 0x100000001b3L
        }
        return h
    }

    fun frameIndex(timeUs: Long): Long = timeUs * FPS / 1_000_000

    /** splitmix64 mixed with [n]; uniform in [0, 1). */
    fun hash01(seed: Long, n: Long): Double {
        var z = seed + n * -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        z = z xor (z ushr 31)
        return (z ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    /** Seed fed to the grain hash for one frame; changes every frame. */
    fun grainSeed(seed: Long, frame: Long): Float = (hash01(seed, frame * 4 + 1) * 1000.0).toFloat()

    fun jitter(seed: Long, frame: Long, amount: Float): FrameJitter = FrameJitter(
        dx = ((hash01(seed, frame * 4 + 2) * 2 - 1) * 0.5 * amount).toFloat(),
        dy = ((hash01(seed, frame * 4 + 3) * 2 - 1) * 0.5 * amount).toFloat(),
        exposure = (1 + (hash01(seed, frame * 4) * 2 - 1) * 0.015 * amount).toFloat(),
    )

    fun hasLeak(seed: Long, probability: Double): Boolean = hash01(seed, -1) < probability

    /** A slow-drifting soft blob entering from one edge of the frame. */
    fun leak(seed: Long, probability: Double, timeSec: Float): LeakState {
        if (!hasLeak(seed, probability)) return LeakState(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val edge = (hash01(seed, -2) * 4).toInt().coerceAtMost(3)
        val along = (0.2 + 0.6 * hash01(seed, -3)).toFloat()
        val period = (6.0 + 6.0 * hash01(seed, -4)).toFloat()
        val phase = (hash01(seed, -5) * 2 * PI).toFloat()
        val drift = 0.12f * sin(2 * PI.toFloat() * timeSec / period + phase)
        val pos = (along + drift).coerceIn(0f, 1f)
        val mix = hash01(seed, -6).toFloat()
        val pulse = 0.8f + 0.2f * sin(2 * PI.toFloat() * timeSec / (period * 0.7f) + phase)
        return LeakState(
            strength = (0.45 + 0.4 * hash01(seed, -7)).toFloat() * pulse,
            x = when (edge) { 0 -> 0f; 1 -> 1f; else -> pos },
            y = when (edge) { 2 -> 0f; 3 -> 1f; else -> pos },
            radius = (0.35 + 0.25 * hash01(seed, -8)).toFloat(),
            r = 1f, g = 0.42f - 0.27f * mix, b = 0.1f + 0.02f * mix,
        )
    }
}
