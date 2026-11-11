package app.sliver.feature.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.sliver.data.files.FileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class AndroidGalleryProvider(private val context: Context, private val files: FileStore) : GalleryProvider {
    private val resolver get() = context.contentResolver

    override suspend fun load(): List<GalleryItem> = withContext(Dispatchers.IO) {
        val zone = TimeZone.currentSystemDefault()
        val hasFavorite = Build.VERSION.SDK_INT >= 30
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DURATION)
            if (hasFavorite) add(MediaStore.MediaColumns.IS_FAVORITE)
        }.toTypedArray()
        val image = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val video = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (" + image + "," + video + ")"
        val result = mutableListOf<GalleryItem>()
        resolver.query(
            MediaStore.Files.getContentUri("external"), projection, selection, null,
            "COALESCE(" + MediaStore.MediaColumns.DATE_TAKEN + ", " + MediaStore.MediaColumns.DATE_ADDED + "*1000) DESC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(projection[0])
            val type = c.getColumnIndexOrThrow(projection[1])
            val taken = c.getColumnIndexOrThrow(projection[2])
            val added = c.getColumnIndexOrThrow(projection[3])
            val dur = c.getColumnIndexOrThrow(projection[4])
            val fav = if (hasFavorite) c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE) else -1
            while (c.moveToNext()) {
                val isVideo = c.getInt(type) == video
                val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val ms = if (c.isNull(taken) || c.getLong(taken) == 0L) c.getLong(added) * 1000 else c.getLong(taken)
                result += GalleryItem(
                    uri = ContentUris.withAppendedId(base, c.getLong(id)).toString(),
                    isVideo = isVideo,
                    date = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).date,
                    durationMs = if (isVideo) c.getLong(dur) else 0L,
                    favorite = fav >= 0 && c.getInt(fav) == 1,
                )
            }
        }
        result
    }

    override suspend fun thumbnail(uri: String, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching { resolver.loadThumbnail(Uri.parse(uri), Size(sizePx, sizePx), null).asImageBitmap() }.getOrNull()
    }

    override suspend fun copyToTmp(uri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val isImage = resolver.getType(source)?.startsWith("image/") == true
        val base = files.newTmpPath(System.currentTimeMillis())
        val path = if (isImage) base.removeSuffix(".mp4") + ".jpg" else base
        val input = checkNotNull(resolver.openInputStream(source)) { "Cannot open media" }
        input.use { i -> java.io.File(path).outputStream().use { o -> i.copyTo(o) } }
        path
    }
}
