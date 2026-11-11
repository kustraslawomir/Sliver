package app.sliver.feature.gallery

import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import app.sliver.data.files.FileStore
import app.sliver.data.media.MotionPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class AndroidGalleryProvider(private val context: Context, private val files: FileStore) : GalleryProvider {
    private val resolver get() = context.contentResolver

    override suspend fun isVideo(uri: String): Boolean = resolver.getType(Uri.parse(uri))?.startsWith("video/") == true

    override suspend fun describe(uris: List<String>): List<GalleryItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { raw ->
            val uri = Uri.parse(raw)
            val video = isVideo(raw)
            val duration = if (video) retrieve(uri, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L else 0L
            val date = (if (video) videoDate(uri) else imageDate(uri)) ?: columnDate(uri)
            date?.let { GalleryItem(raw, video, it, duration) }
        }
    }

    private fun imageDate(uri: Uri): LocalDate? = runCatching {
        resolver.openInputStream(uri)?.use { parseExifDate(ExifInterface(it).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)) }
    }.getOrNull()

    private fun videoDate(uri: Uri): LocalDate? =
        parseVideoDate(retrieve(uri, MediaMetadataRetriever.METADATA_KEY_DATE), TimeZone.currentSystemDefault())

    private fun retrieve(uri: Uri, key: Int): String? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.extractMetadata(key)
        } finally {
            r.release()
        }
    }.getOrNull()

    private fun columnDate(uri: Uri): LocalDate? = runCatching {
        val projection = arrayOf(MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED)
        resolver.query(uri, projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val ms = c.getLong(0).takeIf { it > 0 } ?: (c.getLong(1) * 1000).takeIf { it > 0 }
            ms?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
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
            val input = checkNotNull(resolver.openInputStream(source)) { "Cannot open media" }
            input.use { i -> java.io.File(path).outputStream().use { o -> i.copyTo(o) } }
        } catch (e: Throwable) {
            java.io.File(path).delete()
            throw e
        }
        path
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
