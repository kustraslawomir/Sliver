package app.sliver.data.light

import app.sliver.data.db.DayDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val NO_LUMA = -1f
const val LIGHT_W = 32
const val LIGHT_H = 18

/** Brightness (0..1) and dominant colour (opaque ARGB) of a clip, computed once from three tiny frames. */
data class ClipLight(val luma: Float, val tone: Int)

/** Reads start, middle and end frames of a clip scaled to [LIGHT_W] x [LIGHT_H] as ARGB pixel arrays. */
fun interface ClipLightReader {
    suspend fun read(videoPath: String): ClipLight?
}

/** Mean Rec. 709 luma over every pixel; colour is the mean RGB weighted towards saturated pixels so grey sky does not wash it out. */
fun clipLight(frames: List<IntArray>): ClipLight? {
    var count = 0
    var luma = 0.0
    var wSum = 0.0
    var r = 0.0
    var g = 0.0
    var b = 0.0
    for (frame in frames) for (p in frame) {
        val pr = (p shr 16 and 0xFF) / 255.0
        val pg = (p shr 8 and 0xFF) / 255.0
        val pb = (p and 0xFF) / 255.0
        luma += 0.2126 * pr + 0.7152 * pg + 0.0722 * pb
        val w = (maxOf(pr, pg, pb) - minOf(pr, pg, pb)) + 0.05
        r += pr * w; g += pg * w; b += pb * w; wSum += w
        count++
    }
    if (count == 0) return null
    fun c(v: Double) = (v / wSum * 255).toInt().coerceIn(0, 255)
    return ClipLight((luma / count).toFloat().coerceIn(0f, 1f), (0xFF shl 24) or (c(r) shl 16) or (c(g) shl 8) or c(b))
}

/** Fills luma/tone for clips saved before the columns existed (or restored from a backup), one at a time. */
class LightBackfill(private val dao: DayDao, private val reader: ClipLightReader) {
    suspend fun run() {
        for (day in dao.withoutLight()) {
            val light = reader.read(day.file) ?: continue
            dao.setLight(day.projectId, day.date, light.luma, light.tone)
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch { run() }
    }
}
