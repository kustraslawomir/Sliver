package app.sliver.feature.films

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

private const val UNREADABLE = "This file can't be played. Pick another audio file."

fun trackUri(ref: String): Uri = Uri.fromFile(File(ref))

fun trackExists(ref: String): Boolean = File(ref).isFile

/** Copies the picked document into `filesDir/music` and checks that it has a playable audio stream. */
fun importAudio(context: Context, uri: Uri): PickedAudio {
    val name = displayName(context, uri).replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(80)
    val dir = File(context.filesDir, "music").apply { mkdirs() }
    val out = File(dir, "${System.currentTimeMillis()}_$name")
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return PickedAudio.Failed(UNREADABLE)
        input.use { src -> out.outputStream().use { src.copyTo(it) } }
        if (decodes(out)) {
            PickedAudio.Ok(out.path)
        } else {
            out.delete();
            PickedAudio.Failed(UNREADABLE)
        }
    } catch (e: Exception) {
        out.delete()
        PickedAudio.Failed(UNREADABLE)
    }
}

actual fun audioDurationMs(path: String): Long {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(path)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    } finally {
        r.release()
    }
}

private fun decodes(file: File): Boolean {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(file.path)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes" &&
            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) > 0L
    } catch (e: Exception) {
        false
    } finally {
        r.release()
    }
}

private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "music"
