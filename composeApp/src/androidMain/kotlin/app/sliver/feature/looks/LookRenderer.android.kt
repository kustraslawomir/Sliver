package app.sliver.feature.looks

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.sliver.data.LookParams
import app.sliver.data.info
import app.sliver.data.look.LutCube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private class AndroidLookRenderer(private val context: Context) : LookRenderer {
    private val cubes = ConcurrentHashMap<String, Array<Array<IntArray>>>()

    private fun cube(file: String) = cubes.getOrPut(file) { context.assets.open(file).use { LutCube.parse(it, 1f) } }

    override suspend fun render(frame: ImageBitmap, params: LookParams, maxEdge: Int): ImageBitmap = withContext(Dispatchers.Default) {
        val src = frame.asAndroidBitmap()
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(src.width, src.height))
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src.copy(Bitmap.Config.ARGB_8888, false), w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        val lut = params.preset.info.lutFile?.let(::cube)
        val mix = params.intensity / 100f
        val k = 1f + params.contrast / 100f
        val sat = 1f + params.saturation / 100f
        val fade = params.fade / 100f
        val vig = params.vignette / 100f
        for (y in 0 until h) for (x in 0 until w) {
            val c = px[y * w + x]
            var r = (c shr 16 and 0xFF) / 255f
            var g = (c shr 8 and 0xFF) / 255f
            var b = (c and 0xFF) / 255f
            if (lut != null && mix > 0f) {
                val n = lut.size - 1
                val l = lut[Math.round(r * n)][Math.round(g * n)][Math.round(b * n)]
                r += ((l shr 16 and 0xFF) / 255f - r) * mix
                g += ((l shr 8 and 0xFF) / 255f - g) * mix
                b += ((l and 0xFF) / 255f - b) * mix
            }
            r = (r - 0.5f) * k + 0.5f
            g = (g - 0.5f) * k + 0.5f
            b = (b - 0.5f) * k + 0.5f
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            r = luma + (r - luma) * sat
            g = luma + (g - luma) * sat
            b = luma + (b - luma) * sat
            if (fade > 0f) {
                r = r * (1f - 0.3f * fade) + 0.12f * fade
                g = g * (1f - 0.3f * fade) + 0.12f * fade
                b = b * (1f - 0.3f * fade) + 0.12f * fade
            }
            if (vig > 0f) {
                val dx = (x - w / 2f) / (w / 2f)
                val dy = (y - h / 2f) / (h / 2f)
                val d = sqrt((dx * dx + dy * dy) / 2f)
                val f = 1f - vig * d * d * 0.9f
                r *= f; g *= f; b *= f
            }
            px[y * w + x] = (0xFF shl 24) or (to8(r) shl 16) or (to8(g) shl 8) or to8(b)
        }
        Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    private fun to8(v: Float) = Math.round(v.coerceIn(0f, 1f) * 255f)
}

@Composable
actual fun rememberLookRenderer(): LookRenderer {
    val context = LocalContext.current.applicationContext
    return remember { AndroidLookRenderer(context) }
}
