package app.sliver.feature.gallery

import android.net.Uri
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.sliver.feature.trim.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val EDGE = 192

@Composable
actual fun rememberMediaThumb(uri: String): ImageBitmap? {
    val context = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, uri) {
        val key = ThumbCache.keyFor(uri, "#t")
        ThumbCache.get(key)?.let { value = it.first(); return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.loadThumbnail(Uri.parse(uri), Size(EDGE, EDGE), null).asImageBitmap() }.getOrNull()
        }?.also { ThumbCache.put(key, listOf(it)) }
    }
    return thumb
}
