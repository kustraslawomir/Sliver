package app.sliver.data.files

import kotlinx.datetime.LocalDate
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AndroidFileStore(filesDir: File, cacheDir: File) : FileStore {
    private val root = filesDir
    private val days = File(filesDir, "days").apply { mkdirs() }
    private val thumbs = File(filesDir, "thumbs").apply { mkdirs() }
    private val films = File(filesDir, "films").apply { mkdirs() }
    private val tmp = File(cacheDir, "tmp").apply { mkdirs() }
    private val originals = File(filesDir, "originals")

    override fun dayPath(projectId: String, date: LocalDate) = File(days, "$projectId-$date.mp4").path
    override fun thumbPath(projectId: String, date: LocalDate) = File(thumbs, "$projectId-$date.jpg").path
    override fun filmPath(filmId: String) = File(films, "$filmId.mp4").path
    override fun newTmpPath(timestampMillis: Long) = File(tmp, "rec_$timestampMillis.mp4").path
    override fun exists(path: String) = File(path).exists()
    override fun delete(path: String) = File(path).delete()
    override fun move(from: String, to: String): Boolean {
        val dest = File(to)
        return try {
            Files.move(File(from).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            true
        } catch (e: java.io.IOException) {
            File(from).renameTo(dest)
        }
    }

    override fun freeSpaceBytes() = root.usableSpace

    override fun originalPath(stampMillis: Long): String = File(originals.apply { mkdirs() }, "orig_$stampMillis.mp4").path

    override fun tmpRecordings(): List<TmpRecording> = tmp.listFiles().orEmpty().mapNotNull { f ->
        val start = f.name.removePrefix("rec_").removeSuffix(".mp4").toLongOrNull()
        if (start == null || !f.name.startsWith("rec_") || !f.name.endsWith(".mp4") || f.length() == 0L) return@mapNotNull null
        if (File(f.path + ".jpg").exists()) return@mapNotNull null
        TmpRecording(f.path, start, f.lastModified())
    }

    override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long): Int =
        tmp.listFiles().orEmpty().count { it.isFile && nowMillis - it.lastModified() > maxAgeMillis && it.delete() }
}
