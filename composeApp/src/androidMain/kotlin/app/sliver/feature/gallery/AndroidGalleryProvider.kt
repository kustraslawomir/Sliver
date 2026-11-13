package app.sliver.feature.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import app.sliver.data.files.FileStore
import app.sliver.data.media.MotionPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val YEAR_2000_MS = 946_684_800_000L
private const val HEIC_MAX_EDGE = 4096

// DECISION: Photo Picker uris are not queried for DATE_TAKEN (the picker fills it from the file modified time, the day a messenger saved
// the file) and show their file name as "<id>.jpg", so a file-name hint only exists where the system exposes the real name.
class AndroidGalleryProvider(private val context: Context, private val files: FileStore) : GalleryProvider {
    private val resolver get() = context.contentResolver

    override suspend fun isVideo(uri: String): Boolean = resolver.getType(Uri.parse(uri))?.startsWith("video/") == true

    override suspend fun describe(uris: List<String>): List<GalleryItem> = withContext(Dispatchers.IO) {
        uris.map { raw ->
            val uri = Uri.parse(raw)
            val video = isVideo(raw)
            val duration = if (video) retrieve(uri, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L else 0L
            val zone = TimeZone.currentSystemDefault()
            val name = displayName(uri)
            val taken = (if (video) parseVideoMillis(retrieve(uri, MediaMetadataRetriever.METADATA_KEY_DATE)) else exifMillis(uri, zone))
                ?.takeIf { it >= YEAR_2000_MS }
            val camera = taken?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }
            GalleryItem(raw, video, resolveCaptureDate(camera, name), duration, name, taken)
        }
    }

    private fun exifMillis(uri: Uri, zone: TimeZone): Long? = runCatching {
        resolver.openInputStream(uri)?.use { parseExifMillis(ExifInterface(it).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL), zone) }
    }.getOrNull()

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment

    private fun retrieve(uri: Uri, key: Int): String? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.extractMetadata(key)
        } finally {
            r.release()
        }
    }.getOrNull()

    override suspend fun copyToTmp(uri: String): String = copy(uri, files.newTmpPath(System.currentTimeMillis()))

    override suspend fun keep(uri: String): String {
        val dir = java.io.File(context.filesDir, "backfill").apply { mkdirs() }
        return copy(uri, java.io.File(dir, "${System.nanoTime()}.mp4").path)
    }

    private suspend fun copy(uri: String, base: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val isImage = resolver.getType(source)?.startsWith("image/") == true
        val path = if (isImage) base.removeSuffix(".mp4") + ".jpg" else base
        try {
            if (isHeif(resolver.getType(source))) {
                writeJpeg(source, path)
            } else {
                val input = checkNotNull(resolver.openInputStream(source)) { "Cannot open media" }
                input.use { i -> java.io.File(path).outputStream().use { o -> i.copyTo(o) } }
            }
        } catch (e: Throwable) {
            java.io.File(path).delete()
            throw e
        }
        path
    }

    /** iPhone HEIC/HEIF stills become an upright JPEG in tmp, so Photo clip, export and thumbnails all read a format every device decodes. */
    private fun writeJpeg(source: Uri, path: String) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > HEIC_MAX_EDGE) decoder.setTargetSampleSize((longest + HEIC_MAX_EDGE - 1) / HEIC_MAX_EDGE)
        }
        java.io.File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
    }

    private fun readHead(input: java.io.InputStream): ByteArray {
        val buf = ByteArray(MotionPhoto.HEAD_BYTES)
        var n = 0
        while (n < buf.size) {
            val r = input.read(buf, n, buf.size - n)
            if (r < 0) break
            n += r
        }
        return buf.copyOf(n)
    }

    override suspend fun extractMotionVideo(uri: String): MotionClip? = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        if (resolver.getType(source)?.startsWith("image/") != true) return@withContext null
        val head = resolver.openInputStream(source)?.use { readHead(it) } ?: return@withContext null
        if (!MotionPhoto.declaresMotion(head)) return@withContext null
        val bytes = resolver.openInputStream(source)?.use { it.readBytes() } ?: return@withContext null
        val video = MotionPhoto.find(bytes) ?: return@withContext null
        val path = files.newTmpPath(System.currentTimeMillis())
        java.io.File(path).outputStream().use { it.write(bytes, video.offset, video.length) }
        MotionClip(path, video.presentationUs)
    }
}
