package app.sliver.feature.trim

import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.sliver.data.export.FrameSampler
import app.sliver.feature.camera.displaySize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidTrimMedia(private val sampler: FrameSampler = FrameSampler()) : TrimMedia {
    override suspend fun durationMs(path: String): Long = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            r.release()
        }
    }

    override suspend fun aspect(path: String): Float = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            fun key(k: Int) = r.extractMetadata(k)?.toFloatOrNull() ?: 0f
            val w = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rot = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
            if (w <= 0f || h <= 0f) {
                1f
            } else {
                val (dw, dh) = displaySize(w.toInt(), h.toInt(), rot)
                dw.toFloat() / dh
            }
        } finally {
            r.release()
        }
    }

    override suspend fun autoPickMs(path: String, durationMs: Long): Long =
        if (durationMs <= 1000) 0L else runCatching { sampler.pickBestClipMs(path, durationMs) }.getOrDefault(0L)

    override suspend fun fileSizeBytes(path: String): Long = withContext(Dispatchers.IO) {
        if (path.contains("://")) 0L else java.io.File(path).length()
    }

    override suspend fun thumbnails(path: String, durationMs: Long, count: Int, onEach: (ImageBitmap) -> Unit): List<ImageBitmap> =
        withContext(Dispatchers.IO) {
            val key = ThumbCache.keyFor(path, "#$count")
            ThumbCache.get(key)?.let { cached -> cached.forEach(onEach); return@withContext cached }
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(path)
                val step = durationMs / count
                List(count) { i ->
                    val us = (i * step + step / 2) * 1000
                    val bmp = if (Build.VERSION.SDK_INT >= 27) {
                        r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 160, 160)
                    } else {
                        r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    (bmp?.asImageBitmap() ?: android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap())
                        .also(onEach)
                }.also { ThumbCache.put(key, it) }
            } finally {
                r.release()
            }
        }
}
