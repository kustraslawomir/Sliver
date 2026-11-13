package app.sliver.feature.autoimport

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class AndroidFolderAccess(private val context: Context) : FolderAccess {
    override fun persist(tree: String) {
        context.contentResolver.takePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun release(tree: String) {
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    override fun isGranted(tree: String): Boolean {
        val uri = Uri.parse(tree)
        return context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission } &&
            runCatching { context.contentResolver.query(rootDocument(uri), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { it.moveToFirst() } }
                .getOrNull() == true
    }

    override fun displayName(tree: String): String {
        val uri = Uri.parse(tree)
        return runCatching {
            context.contentResolver.query(rootDocument(uri), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull() ?: "Folder"
    }

    private fun rootDocument(tree: Uri) = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
}

/** Walks the picked tree with the Storage Access Framework and reads each file's real capture date; never writes. */
class AndroidFolderScanner(private val context: Context) : FolderScanner {
    private val resolver get() = context.contentResolver

    override suspend fun scan(tree: String, from: LocalDate, to: LocalDate, zone: TimeZone, onFound: suspend (Int) -> Unit): FolderScanResult =
        withContext(Dispatchers.IO) {
            val treeUri = Uri.parse(tree)
            val collected = collectFiles(DocumentsContract.getTreeDocumentId(treeUri), { children(treeUri, it) }, onFound = onFound)
            val media = collected.files.filter { isMediaName(it.name, it.mime) }
            val out = mutableListOf<MediaEntry>()
            var unreadable = 0
            for (node in media) {
                currentCoroutineContext().ensureActive()
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, node.id)
                val video = isVideoName(node.name, node.mime)
                val entry = runCatching { read(uri, node, video, zone) }.getOrNull()
                if (entry == null) { unreadable++; continue }
                if (localDateOf(entry.takenAtMs, zone) in from..to) out += entry
            }
            FolderScanResult(out, unreadable, collected.truncated)
        }

    private fun children(tree: Uri, dirId: String): List<FolderNode> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val out = mutableListOf<FolderNode>()
        resolver.query(uri, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val mime = c.getString(2)
                out += FolderNode(c.getString(0), c.getString(1).orEmpty(), mime == DocumentsContract.Document.MIME_TYPE_DIR, c.getLong(3), c.getLong(4), mime)
            }
        }
        return out
    }

    private fun read(uri: Uri, node: FolderNode, video: Boolean, zone: TimeZone): MediaEntry? {
        var duration = 0L
        var original: String? = null
        var digitized: String? = null
        var offset: String? = null
        var videoDate: String? = null
        if (video) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, uri)
                duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                videoDate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            } finally {
                r.release()
            }
        } else {
            val input = resolver.openInputStream(uri) ?: return null
            input.use {
                runCatching {
                    val exif = ExifInterface(it)
                    original = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    digitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                    offset = exif.getAttribute("OffsetTimeOriginal") ?: exif.getAttribute("OffsetTimeDigitized")
                }
            }
        }
        val date = resolveItemDate(original, digitized, offset, videoDate, node.name, node.modified, zone) ?: return null
        return MediaEntry(
            uri.toString(), video, date.ms, duration, camera = true, name = node.name, sizeBytes = node.size, fileTime = date.origin == DateOrigin.FILE_TIME,
        )
    }
}
