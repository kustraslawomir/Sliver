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

    override suspend fun copyToTmp(uri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val isImage = resolver.getType(source)?.startsWith("image/") == true
        val base = files.newTmpPath(System.currentTimeMillis())
        val path = if (isImage) base.removeSuffix(".mp4") + ".jpg" else base
        val input = checkNotNull(resolver.openInputStream(source)) { "Cannot open media" }
        input.use { i -> java.io.File(path).outputStream().use { o -> i.copyTo(o) } }
        path
    }

    override suspend fun extractMotionVideo(uri: String): MotionClip? = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        if (resolver.getType(source)?.startsWith("image/") != true) return@withContext null
        runCatching {
            val bytes = resolver.openInputStream(source)?.use { it.readBytes() } ?: return@runCatching null
            val video = MotionPhoto.find(bytes) ?: return@runCatching null
            val path = files.newTmpPath(System.currentTimeMillis())
            java.io.File(path).outputStream().use { it.write(bytes, video.offset, video.length) }
            MotionClip(path, video.presentationUs)
        }.getOrNull()
    }
}
