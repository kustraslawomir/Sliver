package app.sliver.feature.films

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.sliver.data.LookPreset
import app.sliver.data.info
import app.sliver.data.look.LookRaster
import app.sliver.data.look.LutGenerator
import app.sliver.data.look.lookFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val THUMB_W = 144
private const val THUMB_H = 192
private const val THUMB_SEED = 7L

private val tables = ConcurrentHashMap<LookPreset, FloatArray>()
private val rendered = LruCache<String, ImageBitmap>(96)

actual suspend fun renderFilterThumbs(frame: ImageBitmap, presets: List<LookPreset>): Map<LookPreset, ImageBitmap> =
    withContext(Dispatchers.Default) {
        val key = System.identityHashCode(frame)
        var pixels: IntArray? = null
        presets.associateWith { preset ->
            val cacheKey = "$key/${preset.name}"
            rendered.get(cacheKey) ?: run {
                val src = pixels ?: thumbPixels(frame).also { pixels = it }
                val model = preset.info.model
                val out = LookRaster.render(
                    src, THUMB_W, THUMB_H,
                    if (model.hasLut) tables.getOrPut(preset) { LutGenerator.table(model) } else null, 1f,
                    lookFrame(model, 1f, model.grain.toFloat(), THUMB_SEED, 0L, THUMB_H, animate = false),
                )
                Bitmap.createBitmap(out, THUMB_W, THUMB_H, Bitmap.Config.ARGB_8888).asImageBitmap().also { rendered.put(cacheKey, it) }
            }
        }
    }

/** Centre-crops [frame] to the thumbnail's 3:4 and scales it down. */
private fun thumbPixels(frame: ImageBitmap): IntArray {
    val src = frame.asAndroidBitmap().let {
        if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
    }
    val target = THUMB_W.toFloat() / THUMB_H
    val cw: Int
    val ch: Int
    if (src.width.toFloat() / src.height > target) {
        ch = src.height;
        cw = (ch * target).toInt()
    } else {
        cw = src.width;
        ch = (cw / target).toInt()
    }
    val crop = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
    val scaled = Bitmap.createScaledBitmap(crop, THUMB_W, THUMB_H, true)
    return IntArray(THUMB_W * THUMB_H).also { scaled.getPixels(it, 0, THUMB_W, 0, 0, THUMB_W, THUMB_H) }
}
