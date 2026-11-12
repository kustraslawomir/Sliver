package app.sliver.data.files

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.datetime.LocalDate
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.posix.memcpy

fun documentsDir(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String

@OptIn(ExperimentalForeignApi::class)
fun readFileBytes(path: String): ByteArray? {
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun writeFileBytes(path: String, bytes: ByteArray): Boolean = bytes.usePinned {
    NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()).writeToFile(path, atomically = true)
}

@OptIn(ExperimentalForeignApi::class)
class IosFileStore(root: String = documentsDir(), cache: String = NSTemporaryDirectory()) : FileStore {
    private val fm = NSFileManager.defaultManager
    private val root = root
    private val days = dir("$root/days")
    private val thumbs = dir("$root/thumbs")
    private val films = dir("$root/films")
    private val tmp = dir("${cache.trimEnd('/')}/tmp")

    private fun dir(path: String) = path.also { fm.createDirectoryAtPath(it, true, null, null) }

    override fun dayPath(projectId: String, date: LocalDate) = "$days/$projectId-$date.mp4"
    override fun thumbPath(projectId: String, date: LocalDate) = "$thumbs/$projectId-$date.jpg"
    override fun filmPath(filmId: String) = "$films/$filmId.mp4"
    override fun newTmpPath(timestampMillis: Long) = "$tmp/rec_$timestampMillis.mp4"
    override fun exists(path: String) = fm.fileExistsAtPath(path)
    override fun delete(path: String) = fm.removeItemAtPath(path, null)
    override fun move(from: String, to: String): Boolean {
        if (fm.fileExistsAtPath(to)) fm.removeItemAtPath(to, null)
        return fm.moveItemAtPath(from, to, null)
    }

    override fun freeSpaceBytes(): Long =
        (fm.attributesOfFileSystemForPath(root, null)?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: 0L

    override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long): Int =
        fm.contentsOfDirectoryAtPath(tmp, null).orEmpty().map { "$tmp/$it" }.count { path ->
            val modified = fm.attributesOfItemAtPath(path, null)?.get(NSFileModificationDate) as? NSDate
            modified != null && nowMillis - (modified.timeIntervalSince1970 * 1000).toLong() > maxAgeMillis && delete(path)
        }
}
