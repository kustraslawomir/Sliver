package app.sliver.data.export

import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import kotlin.math.ceil

const val SECOND_MS = 1000L

enum class ShortMode { SLOW_TO_FIT, LOOP, HOLD_LAST }

data class ExportedClip(val videoPath: String, val thumbPath: String, val durationMs: Long, val color: Int = 0)

interface ClipExporter {
    /**
     * Exports exactly one second of [source] starting at [startMs] into [videoPath] (1080p H.264/AAC)
     * and a 320px JPEG thumbnail into [thumbPath]. [speed] 0.5 takes 500 ms and stretches it to 1 s.
     */
    suspend fun exportSecond(
        source: String,
        startMs: Long,
        speed: Float,
        cropY: Float?,
        format: FilmFormat,
        videoPath: String,
        thumbPath: String,
        shortMode: ShortMode = ShortMode.SLOW_TO_FIT,
        look: LookParams = LookParams(),
    ): ExportedClip

    /** Turns the still [source] into a 1.000 s, 30 fps clip; ZOOM eases scale 1.00 to 1.08 over the second. */
    suspend fun photoToSecond(
        source: String,
        motion: Motion,
        format: FilmFormat,
        videoPath: String,
        thumbPath: String,
        look: LookParams = LookParams(),
    ): ExportedClip
}

object PhotoMotion {
    const val FPS = 30
    const val MAX_ZOOM = 1.08f

    /** Smoothstep-eased zoom scale at [progress] 0..1 of the second. */
    fun zoomScale(progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return 1f + (MAX_ZOOM - 1f) * t * t * (3f - 2f * t)
    }
}

/** Which part of the source feeds the 1 s output and how it is filled when the source is too short. */
data class ClipPlan(
    val startMs: Long,
    val endMs: Long,
    val speed: Float,
    val loops: Int = 1,
    val holdMs: Long = 0,
)

object ClipPlanner {
    fun plan(sourceDurationMs: Long, startMs: Long, speed: Float, mode: ShortMode): ClipPlan {
        val needed = (SECOND_MS * speed).toLong().coerceAtLeast(1)
        if (sourceDurationMs >= needed) {
            val start = startMs.coerceIn(0, sourceDurationMs - needed)
            return ClipPlan(start, start + needed, speed)
        }
        return when (mode) {
            ShortMode.SLOW_TO_FIT -> ClipPlan(0, sourceDurationMs, sourceDurationMs.toFloat() / SECOND_MS)
            ShortMode.LOOP -> {
                val pass = sourceDurationMs / speed
                ClipPlan(0, sourceDurationMs, speed, loops = ceil(SECOND_MS / pass).toInt().coerceAtLeast(1))
            }
            ShortMode.HOLD_LAST -> {
                val pass = (sourceDurationMs / speed).toLong()
                ClipPlan(0, sourceDurationMs, speed, holdMs = (SECOND_MS - pass).coerceAtLeast(0))
            }
        }
    }

    /** Normalized (-1..1) crop rect for fitting a source of [srcAspect] into [targetAspect]; cropY 0..1, null = center. */
    fun cropRect(srcAspect: Float, targetAspect: Float, cropY: Float?): CropRect {
        if (srcAspect >= targetAspect) {
            val f = targetAspect / srcAspect
            return CropRect(-f, f, -1f, 1f)
        }
        val f = srcAspect / targetAspect
        val top = 1f - 2f * (cropY ?: 0.5f).coerceIn(0f, 1f) * (1f - f)
        return CropRect(-1f, 1f, top - 2f * f, top)
    }
}

data class CropRect(val left: Float, val right: Float, val bottom: Float, val top: Float)

/** AAC frames are 1024 samples, so a 1 s clip cannot hold a fractional frame; whole frames must end by the limit. */
object AudioFit {
    const val AAC_FRAME_SAMPLES = 1024

    fun frameUs(sampleRate: Int): Long = AAC_FRAME_SAMPLES * 1_000_000L / sampleRate

    /** True when an audio sample starting at [ptsUs] still ends within [limitUs]. */
    fun keep(ptsUs: Long, sampleRate: Int, limitUs: Long = SECOND_MS * 1000): Boolean = ptsUs + frameUs(sampleRate) <= limitUs
}
