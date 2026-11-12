package app.sliver.feature.trim

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

private const val REPAIR_WIDTH = 320

/** A day thumbnail `.../thumbs/<project>-<date>.jpg` belongs to the video `.../days/<project>-<date>.mp4`; null for any other path. */
internal fun videoForThumb(thumbPath: String): String? {
    if (!thumbPath.endsWith(".jpg")) return null
    val file = File(thumbPath)
    if (file.parentFile?.name != "thumbs") return null
    return File(File(file.parentFile!!.parentFile, "days"), file.nameWithoutExtension + ".mp4").path
}

/** Rewrites a missing or unreadable day thumbnail from its video so a filled day never shows an empty box. */
internal fun repairThumb(thumbPath: String): Boolean {
    val video = videoForThumb(thumbPath)?.let(::File)?.takeIf { it.length() > 0 } ?: return false
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(video.path)
        val frame = r.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST) ?: return false
        val thumb = Bitmap.createScaledBitmap(frame, REPAIR_WIDTH, (REPAIR_WIDTH.toFloat() * frame.height / frame.width).toInt().coerceAtLeast(1), true)
        val tmp = File(thumbPath + ".tmp")
        tmp.outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        tmp.renameTo(File(thumbPath))
    } catch (e: RuntimeException) {
        false
    } finally {
        r.release()
    }
}
