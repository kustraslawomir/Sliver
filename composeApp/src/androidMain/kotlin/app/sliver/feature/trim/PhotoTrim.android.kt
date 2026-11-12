package app.sliver.feature.trim

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_EDGE = 1600

@Composable
actual fun rememberPhoto(path: String): ImageBitmap? {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, path) {
        val key = ThumbCache.keyFor(path)
        ThumbCache.get(key)?.let { value = it.first(); return@produceState }
        fun decode() = runCatching {
            val uri = if (path.contains("://")) Uri.parse(path) else Uri.fromFile(File(path))
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_EDGE) decoder.setTargetSampleSize((longest + MAX_EDGE - 1) / MAX_EDGE)
            }.asImageBitmap()
        }.getOrNull()
        value = withContext(Dispatchers.IO) {
            decode() ?: if (repairThumb(path)) decode() else null
        }?.also { ThumbCache.put(key, listOf(it)) }
    }
    return photo
}
