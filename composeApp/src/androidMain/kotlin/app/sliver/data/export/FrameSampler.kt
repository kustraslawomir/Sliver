package app.sliver.data.export

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import app.sliver.core.log.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Luminance-only frame sampler feeding [bestClip]. */
class FrameSampler {
    private companion object { const val WIDTH = 160 }

    suspend fun pickBestClipMs(path: String, durationMs: Long): Long = withContext(Dispatchers.Default) {
        val t0 = SystemClock.elapsedRealtime()
        val fallback = SamplingPlan.fallbackStartMs(durationMs)
        val result = withTimeoutOrNull(SamplingPlan.TIMEOUT_MS) { score(path, durationMs) }
        if (result == null) {
            logDebug("FrameSampler", "timed out, using middle of ${durationMs}ms clip")
        } else {
            logDebug("FrameSampler", "scored ${durationMs}ms clip in ${SystemClock.elapsedRealtime() - t0}ms")
        }
        (result ?: fallback).coerceIn(0, (durationMs - SECOND_MS).coerceAtLeast(0))
    }

    private suspend fun score(path: String, durationMs: Long): Long {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            if (!SamplingPlan.isCoarse(durationMs)) {
                return bestClip(sample(r, 0, durationMs, SamplingPlan.FINE_FPS), SamplingPlan.FINE_FPS)
            }
            val step = SamplingPlan.coarseStepMs(durationMs)
            val fps = SamplingPlan.coarseWindowFps(step)
            val coarse = bestClip(sample(r, 0, durationMs, stepMs = step), fps) * step * fps / SECOND_MS
            val range = SamplingPlan.refineRange(coarse, durationMs)
            return range.first + bestClip(sample(r, range.first, range.last, SamplingPlan.FINE_FPS), SamplingPlan.FINE_FPS)
        } finally {
            r.release()
        }
    }

    private suspend fun sample(r: MediaMetadataRetriever, fromMs: Long, toMs: Long, fps: Int = 0, stepMs: Long = 1000L / fps): List<FloatArray> {
        val step = stepMs
        val out = ArrayList<FloatArray>()
        var t = fromMs
        while (t <= toMs) {
            currentCoroutineContext().ensureActive()
            luma(r, t)?.let { out += it }
            t += step
        }
        return out
    }

    private fun luma(r: MediaMetadataRetriever, ms: Long): FloatArray? {
        val bmp = (
            if (Build.VERSION.SDK_INT >= 27) {
                r.getScaledFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, WIDTH, WIDTH * 9 / 16)
            } else {
                r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            ) ?: return null
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
}
