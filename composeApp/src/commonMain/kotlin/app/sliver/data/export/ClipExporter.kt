package app.sliver.data.export

import app.sliver.data.FilmFormat
import kotlin.math.ceil

const val SECOND_MS = 1000L

enum class ShortMode { SLOW_TO_FIT, LOOP, HOLD_LAST }

data class ExportedClip(val videoPath: String, val thumbPath: String, val durationMs: Long)

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
    ): ExportedClip
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
