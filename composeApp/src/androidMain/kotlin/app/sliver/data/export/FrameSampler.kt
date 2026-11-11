package app.sliver.data.export

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Luminance-only frame sampler feeding [bestSecond]. */
class FrameSampler {
    suspend fun pickBestSecondMs(path: String, durationMs: Long): Long = withContext(Dispatchers.Default) {
        val t0 = SystemClock.elapsedRealtime()
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            val result = if (durationMs > LONG_CLIP_MS) {
                val coarse = bestSecond(sample(r, 0, durationMs, COARSE_FPS), COARSE_FPS)
                val from = (coarse - REFINE_MS).coerceAtLeast(0)
                val to = (coarse + SECOND_MS + REFINE_MS).coerceAtMost(durationMs)
                from + bestSecond(sample(r, from, to, FPS), FPS)
            } else {
                bestSecond(sample(r, 0, durationMs, FPS), FPS)
            }
            Log.d("FrameSampler", "scored ${durationMs}ms clip in ${SystemClock.elapsedRealtime() - t0}ms")
            result.coerceIn(0, (durationMs - SECOND_MS).coerceAtLeast(0))
        } finally {
            r.release()
        }
    }

    private fun sample(r: MediaMetadataRetriever, fromMs: Long, toMs: Long, fps: Int): List<FloatArray> {
        val step = 1000L / fps
        val out = ArrayList<FloatArray>()
        var t = fromMs
        while (t <= toMs) {
            luma(r, t)?.let { out += it }
            t += step
        }
        return out
    }

    private fun luma(r: MediaMetadataRetriever, ms: Long): FloatArray? {
        val bmp = (if (Build.VERSION.SDK_INT >= 27) {
            r.getScaledFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, WIDTH, WIDTH * 9 / 16)
        } else {
            r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }) ?: return null
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        bmp.recycle()
        return FloatArray(px.size) {
            val c = px[it]
            0.299f * (c shr 16 and 0xFF) + 0.587f * (c shr 8 and 0xFF) + 0.114f * (c and 0xFF)
        }
    }

    private companion object {
        const val WIDTH = 160
        const val FPS = 10
        const val COARSE_FPS = 2
        const val LONG_CLIP_MS = 60_000L
        const val REFINE_MS = 5_000L
    }
}
