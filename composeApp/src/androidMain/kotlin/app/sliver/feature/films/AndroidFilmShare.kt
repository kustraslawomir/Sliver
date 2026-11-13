package app.sliver.feature.films

import app.sliver.core.APP_NAME

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFilmShare(private val context: Context) : FilmShare {
    override fun share(path: String, name: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun sizeBytes(path: String): Long = File(path).length()

    override suspend fun saveToPhotos(path: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        if (alreadySaved(name, File(path).length())) return@withContext true
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${freeName(name)}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$APP_NAME")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { out -> File(path).inputStream().use { it.copyTo(out) } }
                ?: error("No output stream")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    /** The same render saved again (same name, same size) is not copied a second time. */
    private fun alreadySaved(name: String, size: Long): Boolean = runCatching {
        val base = name.trim().ifEmpty { "Film" }.replace(Regex("[\\\\/:*?\"<>|]"), "_").lowercase()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media.DISPLAY_NAME),
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Video.Media.SIZE} = ? AND ${MediaStore.Video.Media.IS_PENDING} = 0",
            arrayOf("${Environment.DIRECTORY_MOVIES}/$APP_NAME%", size.toString()), null,
        )?.use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.any { it.lowercase().startsWith(base) } } ?: false
    }.getOrDefault(false)

    /** Owner: a film rendered again does not overwrite the old one: "Lena", "Lena 2", "Lena 3"… in Movies/<app>. */
    private fun freeName(name: String): String {
        val base = name.trim().ifEmpty { "Film" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val taken = HashSet<String>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media.DISPLAY_NAME),
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?", arrayOf("${Environment.DIRECTORY_MOVIES}/$APP_NAME%"), null,
            )?.use { c -> while (c.moveToNext()) c.getString(0)?.let { taken += it.substringBeforeLast('.').lowercase() } }
        }
        return filmFileName(base, taken)
    }
}
