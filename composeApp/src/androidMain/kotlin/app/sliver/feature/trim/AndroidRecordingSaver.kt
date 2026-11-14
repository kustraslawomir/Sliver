package app.sliver.feature.trim

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import app.sliver.core.APP_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full recordings land in Movies/<app> as "<app>_2026-11-13_145900.mp4" (named by when recording started). */
class AndroidRecordingSaver(private val context: Context) : RecordingSaver {
    private val folder = "${Environment.DIRECTORY_MOVIES}/$APP_NAME"

    override suspend fun save(path: String, stampMillis: Long): Boolean = withContext(Dispatchers.IO) {
        val source = File(path)
        if (!source.exists()) return@withContext false
        val name = "${APP_NAME}_${SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date(stampMillis))}.mp4"
        val resolver = context.contentResolver
        val already = runCatching {
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media.SIZE),
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Video.Media.DISPLAY_NAME} = ?", arrayOf("$folder%", name), null,
            )?.use { c -> c.moveToFirst() && c.getLong(0) == source.length() } == true
        }.getOrDefault(false)
        if (already) return@withContext true
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, folder)
            put(MediaStore.Video.Media.DATE_TAKEN, stampMillis)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } } ?: error("No output stream")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
