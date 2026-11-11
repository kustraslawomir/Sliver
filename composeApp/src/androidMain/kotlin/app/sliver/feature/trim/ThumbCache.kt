package app.sliver.feature.trim

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/** Memory cache shared by the calendar cells and the Edit clip strip; sized by bitmap bytes. Day thumbnails are already JPEGs on disk. */
object ThumbCache {
    private val cache = object : LruCache<String, List<ImageBitmap>>((Runtime.getRuntime().maxMemory() / 16).toInt().coerceAtLeast(4 shl 20)) {
        override fun sizeOf(key: String, value: List<ImageBitmap>) = value.sumOf { it.width * it.height * 4 }
    }

    fun get(key: String): List<ImageBitmap>? = cache.get(key)

    fun put(key: String, value: List<ImageBitmap>) {
        cache.put(key, value)
    }

    fun keyFor(path: String, suffix: String = ""): String {
        val modified = if (path.contains("://")) 0L else java.io.File(path).lastModified()
        return "$path@$modified$suffix"
    }
}
