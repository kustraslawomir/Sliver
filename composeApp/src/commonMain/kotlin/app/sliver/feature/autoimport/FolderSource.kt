package app.sliver.feature.autoimport

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.sliver.feature.gallery.filenameDateHint
import app.sliver.feature.gallery.parseExifMillis
import app.sliver.feature.gallery.parseVideoMillis
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

const val MAX_FOLDER_DEPTH = 8
const val MAX_FOLDER_FILES = 20_000
private const val LIVE_VIDEO_MAX_MS = 6_000L
private const val YEAR_2000_MS = 946_684_800_000L

/** Where a folder item's date came from, best first. FILE_TIME is only the copy time, so the user is told. */
enum class DateOrigin { EXIF, VIDEO, FILENAME, FILE_TIME }

data class ResolvedDate(val ms: Long, val origin: DateOrigin)

private val offsetPattern = Regex("""^([+-])(\d{2}):?(\d{2})$""")

/** EXIF OffsetTime ('+02:00') in milliseconds, or null when absent or malformed. */
fun parseExifOffsetMs(raw: String?): Long? {
    val m = raw?.trim()?.let { offsetPattern.find(it) } ?: return null
    val (sign, h, mi) = m.destructured
    val ms = (h.toInt() * 60L + mi.toInt()) * 60_000
    return if (sign == "-") -ms else ms
}

/** EXIF local time as epoch millis; with an offset tag the camera's own zone wins over the device zone. */
fun exifMillisWithOffset(raw: String?, offsetRaw: String?, zone: TimeZone): Long? {
    val offset = parseExifOffsetMs(offsetRaw) ?: return parseExifMillis(raw, zone)
    val asUtc = parseExifMillis(raw, TimeZone.UTC) ?: return null
    return asUtc - offset
}

/**
 * Capture date of a copied file: EXIF (DateTimeOriginal, then DateTimeDigitized), container creation time, file name, and
 * only then the file's modified time (a copy sets that to the copy day).
 */
fun resolveItemDate(
    exifOriginal: String?, exifDigitized: String?, exifOffset: String?, videoDate: String?, name: String?, lastModified: Long, zone: TimeZone,
): ResolvedDate? {
    val exif = exifMillisWithOffset(exifOriginal, exifOffset, zone) ?: exifMillisWithOffset(exifDigitized, exifOffset, zone)
    if (exif != null && exif >= YEAR_2000_MS) return ResolvedDate(exif, DateOrigin.EXIF)
    val video = parseVideoMillis(videoDate)
    if (video != null && video >= YEAR_2000_MS) return ResolvedDate(video, DateOrigin.VIDEO)
    filenameDateHint(name)?.let { return ResolvedDate(it.atNoon(zone), DateOrigin.FILENAME) }
    return if (lastModified >= YEAR_2000_MS) ResolvedDate(lastModified, DateOrigin.FILE_TIME) else null
}

private fun LocalDate.atNoon(zone: TimeZone): Long = LocalDateTime(year, month.ordinal + 1, day, 12, 0, 0).toInstant(zone).toEpochMilliseconds()

/** A file or directory as a folder lister reports it. */
data class FolderNode(val id: String, val name: String, val dir: Boolean, val size: Long = 0, val modified: Long = 0, val mime: String? = null)

fun interface FolderLister {
    suspend fun children(dirId: String): List<FolderNode>
}

data class Collected(val files: List<FolderNode>, val truncated: Boolean)

/** Breadth-first walk under [maxDepth] and [maxFiles]; a directory that cannot be listed is skipped. */
suspend fun collectFiles(
    root: String, lister: FolderLister, maxDepth: Int = MAX_FOLDER_DEPTH, maxFiles: Int = MAX_FOLDER_FILES, onFound: suspend (Int) -> Unit = {},
): Collected {
    val files = mutableListOf<FolderNode>()
    var level = listOf(root)
    var truncated = false
    var depth = 0
    while (level.isNotEmpty() && !truncated) {
        val next = mutableListOf<String>()
        for (dir in level) {
            currentCoroutineContext().ensureActive()
            for (node in runCatching { lister.children(dir) }.getOrDefault(emptyList())) {
                if (node.dir) {
                    if (depth < maxDepth) next += node.id else truncated = true
                } else {
                    if (files.size >= maxFiles) { truncated = true; break }
                    files += node
                }
            }
            onFound(files.size)
            if (truncated && files.size >= maxFiles) break
        }
        level = next
        depth++
    }
    return Collected(files, truncated)
}

fun isMediaName(name: String, mime: String?): Boolean {
    if (mime?.startsWith("image/") == true || mime?.startsWith("video/") == true) return true
    return name.substringAfterLast('.', "").lowercase() in MediaExtensions
}

fun isVideoName(name: String, mime: String?): Boolean =
    mime?.startsWith("video/") == true || (mime?.startsWith("image/") != true && name.substringAfterLast('.', "").lowercase() in VideoExtensions)

private val VideoExtensions = setOf("mp4", "mov", "m4v", "3gp", "mkv", "webm", "hevc")
private val MediaExtensions = VideoExtensions + setOf("jpg", "jpeg", "png", "heic", "heif", "webp")

/** Two copies of one file (same name and size) in different sub-folders count once. */
fun dedupeByNameAndSize(entries: List<MediaEntry>): List<MediaEntry> = entries.distinctBy { it.name?.lowercase() to it.sizeBytes }

/**
 * A Live Photo is a HEIC/JPEG plus a few seconds of video with the same base name. The video is part of the photo, not a clip,
 * so it is dropped when its still is present.
 */
fun dropLivePhotoVideos(entries: List<MediaEntry>): List<MediaEntry> {
    fun base(e: MediaEntry) = e.name?.substringBeforeLast('.')?.lowercase()
    val stills = entries.filter { !it.video }.mapNotNull(::base).toSet()
    return entries.filterNot { it.video && it.durationMs <= LIVE_VIDEO_MAX_MS && base(it) in stills }
}

class FolderScanResult(val entries: List<MediaEntry>, val unreadable: Int, val truncated: Boolean)

/** Reads dated media from a folder the user picked. Never modifies it. */
interface FolderScanner {
    suspend fun scan(tree: String, from: LocalDate, to: LocalDate, zone: TimeZone, onFound: suspend (Int) -> Unit): FolderScanResult
}

/** Entries inside [from]..[to] from a folder scan, with duplicates and Live Photo videos removed. */
fun cleanFolderEntries(entries: List<MediaEntry>): List<MediaEntry> = dropLivePhotoVideos(dedupeByNameAndSize(entries))

/** Chosen folder: tree uri (with the persisted grant) and its name. */
data class FolderSource(val tree: String, val name: String)

interface FolderAccess {
    /** Keeps read access to [tree] across restarts. */
    fun persist(tree: String)
    fun release(tree: String)
    fun isGranted(tree: String): Boolean
    fun displayName(tree: String): String
}

class FolderSourceStore(private val store: DataStore<Preferences>) {
    private val treeKey = stringPreferencesKey("folderTree")
    private val nameKey = stringPreferencesKey("folderName")

    val folder: Flow<FolderSource?> = store.data.map { p -> p[treeKey]?.let { FolderSource(it, p[nameKey].orEmpty()) } }

    suspend fun current(): FolderSource? = folder.first()

    suspend fun set(source: FolderSource) {
        store.edit { it[treeKey] = source.tree; it[nameKey] = source.name }
    }

    suspend fun forget() {
        store.edit { it.remove(treeKey); it.remove(nameKey) }
    }
}

fun folderSummary(unreadable: Int, fileTime: Int, truncated: Boolean): List<String> = buildList {
    if (unreadable > 0) add("Could not read $unreadable ${if (unreadable == 1) "item" else "items"}")
    if (fileTime > 0) add("$fileTime ${if (fileTime == 1) "item has" else "items have"} no capture date, so the file date was used")
    if (truncated) add("Only the first ${groupThousands(MAX_FOLDER_FILES)} files were checked")
}

const val FILE_TIME_NOTE = "Date from file time"
