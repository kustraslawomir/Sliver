package app.sliver.data.light

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Start, middle and end frames of the one-second clip, scaled to 32 x 18 px. */
object AndroidClipLight : ClipLightReader {
    private val framesUs = listOf(0L, 500_000L, 950_000L)

    override suspend fun read(videoPath: String): ClipLight? = withContext(Dispatchers.Default) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(videoPath)
            val frames = framesUs.mapNotNull { us ->
                r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, LIGHT_W, LIGHT_H)?.let { bmp ->
                    IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height); bmp.recycle() }
                }
            }
            clipLight(frames)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }
}
