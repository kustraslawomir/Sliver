package app.sliver

import app.sliver.data.export.bestClip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoPickTest {
    private fun frames(n: Int, level: (Int) -> Float) = List(n) { floatArrayOf(level(it), level(it)) }

    private fun covers(startMs: Long, diff: Int) = diff.toLong() in (startMs / 100)..(startMs / 100 + 9)

    @Test fun staticPicksMiddle() {
        val s = bestClip(frames(50) { 5f }, 10)
        assertTrue(s in 1800..2000)
    }

    @Test fun singleSpike() {
        val s = bestClip(frames(50) { if (it == 40) 100f else 0f }, 10)
        assertTrue(covers(s, 39) && covers(s, 40))
    }

    @Test fun twoSpikesPicksBigger() {
        val s = bestClip(frames(60) { when (it) { 10 -> 20f; 50 -> 100f; else -> 0f } }, 10)
        assertTrue(covers(s, 49))
    }

    @Test fun equalSpikesTieGoesNearerMiddle() {
        val s = bestClip(frames(60) { if (it == 5 || it == 50) 100f else 0f }, 10)
        assertTrue(s > 2000)
    }

    @Test fun shortClipReturnsZero() {
        assertEquals(0L, bestClip(frames(5) { it.toFloat() }, 10))
    }
}
