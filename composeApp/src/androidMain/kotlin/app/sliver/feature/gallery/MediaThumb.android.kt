package app.sliver.feature.gallery

import android.content.Context
import android.media.MediaMetadataRetriever
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
            val u = Uri.parse(uri)
            runCatching { context.contentResolver.loadThumbnail(u, Size(EDGE, EDGE), null).asImageBitmap() }.getOrNull()
                ?: videoFrame(context, u)
        }?.also { ThumbCache.put(key, listOf(it)) }
    }
    return thumb
}

/** Folder (document) uris of videos often have no system thumbnail: take a small frame from the video itself. */
private fun videoFrame(context: Context, uri: Uri): ImageBitmap? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        r.getScaledFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, EDGE, EDGE)?.asImageBitmap()
    } catch (e: Exception) {
        null
    } finally {
        runCatching { r.release() }
    }
}
