package app.sliver.data.files

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.datetime.LocalDate

class AndroidFileStore(filesDir: File, cacheDir: File) : FileStore {
    private val root = filesDir
    private val days = File(filesDir, "days").apply { mkdirs() }
    private val thumbs = File(filesDir, "thumbs").apply { mkdirs() }
    private val films = File(filesDir, "films").apply { mkdirs() }
    private val tmp = File(cacheDir, "tmp").apply { mkdirs() }

    override fun dayPath(date: LocalDate) = File(days, "$date.mp4").path
    override fun thumbPath(date: LocalDate) = File(thumbs, "$date.jpg").path
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

    override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long): Int =
        tmp.listFiles().orEmpty().count { it.isFile && nowMillis - it.lastModified() > maxAgeMillis && it.delete() }
}
