package app.sliver.feature.autoimport

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.sliver.core.log.logDebug
import app.sliver.core.time.todayLocal
import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.files.FileStore
import app.sliver.data.media.MotionPhoto
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.trim.TrimMedia
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

/** Reads dated gallery items. Needs the full or partial media permission; with partial access only visible items come back. */
interface MediaScanner {
    /** Items taken in [from]..[to] (device [zone]); motion photos are only detected on [sniffDays]. */
    suspend fun scan(from: LocalDate, to: LocalDate, zone: TimeZone, sniffDays: Set<LocalDate>, onFound: (Int) -> Unit = {}): List<MediaEntry>
}

/** Runs [AutoImportExecutor.run] in the background, surviving app close. */
interface AutoImportRunner {
    fun start()
    fun cancel()
}

/** The latest import run, persisted so work resumes and Undo all can find it. */
class AutoImportStore(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("autoImportRun")

    val run: Flow<ImportRun?> = store.data.map { it[key]?.let(::decode) }

    suspend fun current(): ImportRun? = run.first()

    suspend fun start(run: ImportRun) {
        store.edit { it[key] = Json.encodeToString(ImportRun.serializer(), run) }
    }

    /** False when the run was cleared or replaced meanwhile. */
    suspend fun update(id: String, change: (ImportRun) -> ImportRun): Boolean {
        var alive = false
        store.edit { prefs ->
            val cur = prefs[key]?.let(::decode)?.takeIf { it.id == id } ?: return@edit
            alive = true
            prefs[key] = Json.encodeToString(ImportRun.serializer(), change(cur))
        }
        return alive
    }

    private val dismissedKey = stringPreferencesKey("autoImportDismissed")
    private val mapSer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

    /**
     * Gallery items of auto-filled days the user removed, per "projectId|date". Owner: a removed day must not come back
     * from the same files on the next import, only when the gallery has new items for that day.
     */
    suspend fun dismissed(): Map<String, List<String>> =
        store.data.first()[dismissedKey]?.let { runCatching { Json.decodeFromString(mapSer, it) }.getOrNull() }.orEmpty()

    suspend fun dismiss(projectId: String, date: LocalDate, uris: Collection<String>) = editDismissed { m ->
        val k = dismissKey(projectId, date)
        m[k] = (m[k].orEmpty() + uris).distinct()
    }

    suspend fun undismiss(projectId: String, date: LocalDate, uris: Collection<String>) = editDismissed { m ->
        val k = dismissKey(projectId, date)
        val left = m[k].orEmpty() - uris.toSet()
        if (left.isEmpty()) m.remove(k) else m[k] = left
    }

    private suspend fun editDismissed(change: (MutableMap<String, List<String>>) -> Unit) {
        store.edit { prefs ->
            val m = prefs[dismissedKey]?.let { runCatching { Json.decodeFromString(mapSer, it) }.getOrNull() }.orEmpty().toMutableMap()
            change(m)
            prefs[dismissedKey] = Json.encodeToString(mapSer, m)
        }
    }

    private val altKey = stringPreferencesKey("autoImportAlternatives")
    private val altSer = MapSerializer(String.serializer(), ListSerializer(DraftCandidate.serializer()))

    /**
     * Owner: after adding files (e.g. from an iPhone folder), days that already have a clip show that there are other
     * items to pick. Kept here, not on the day row, so sync does not re-upload every clip. Key "projectId|date".
     */
    val alternatives: Flow<Map<String, List<DraftCandidate>>> =
        store.data.map { p -> p[altKey]?.let { runCatching { Json.decodeFromString(altSer, it) }.getOrNull() }.orEmpty() }

    suspend fun addAlternatives(projectId: String, date: LocalDate, items: List<DraftCandidate>) = editAlt { m ->
        val k = dismissKey(projectId, date)
        m[k] = (m[k].orEmpty() + items).distinctBy { it.uri }.take(MAX_CANDIDATES)
    }

    suspend fun clearAlternatives(projectId: String, date: LocalDate) = editAlt { it.remove(dismissKey(projectId, date)) }

    private suspend fun editAlt(change: (MutableMap<String, List<DraftCandidate>>) -> Unit) {
        store.edit { prefs ->
            val m = prefs[altKey]?.let { runCatching { Json.decodeFromString(altSer, it) }.getOrNull() }.orEmpty().toMutableMap()
            change(m)
            prefs[altKey] = Json.encodeToString(altSer, m)
        }
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }

    private fun decode(raw: String) = runCatching { Json.decodeFromString(ImportRun.serializer(), raw) }.getOrNull()
}

class AutoImportExecutor(
    private val store: AutoImportStore,
    private val days: DayRepository,
    private val projects: ProjectRepository,
    private val scanner: MediaScanner,
    private val gallery: GalleryProvider,
    private val media: TrimMedia,
    private val files: FileStore,
    private val today: () -> LocalDate = ::todayLocal,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val folderScanner: FolderScanner? = null,
) {
    /** Counts of days skipped because their item could not be read, for the summary. */
    var failed = 0
        private set

    /** Makes one draft per gap day that has gallery items. Resumable: days that already have a row are skipped. */
    /**
     * Drafts whose picked item the user had already rejected (e.g. rejected from a folder, found again in the gallery)
     * are removed; the day stays empty. Returns how many.
     */
    suspend fun pruneDismissedDrafts(): Int {
        val dismissed = store.dismissed()
        if (dismissed.isEmpty()) return 0
        var n = 0
        for (day in days.observeDrafts().first()) {
            val seen = dismissed[dismissKey(day.projectId, day.date)] ?: continue
            val picked = decodeCandidates(day.draftCandidates).firstOrNull() ?: continue
            val name = picked.name ?: runCatching { gallery.nameOf(picked.uri) }.getOrNull()
            if (!isDismissed(picked.uri, name, seen)) continue
            days.deleteRow(day.projectId, day.date)
            files.delete(day.file)
            files.delete(day.thumb)
            n++
        }
        return n
    }

    suspend fun run() {
        runCatching { pruneDismissedDrafts() }
        val run = store.current()?.takeIf { !it.finished } ?: return
        failed = 0
        val from = LocalDate.parse(run.from)
        val to = LocalDate.fromEpochDays(today().toEpochDays() - 1)
        val known = days.draftsOfRun(run.projectId, run.id).size
        val gaps = datesBetween(from, to).filter { days.getDay(run.projectId, it) == null }
        store.update(run.id) { it.copy(gapDays = gaps.size, startedAt = it.startedAt.takeIf { s -> s > 0 } ?: nowMillis()) }
        val found = if (run.folder != null) scanFolder(run, from, to) else scanGallery(run, from, to, gaps.toSet())
        val dismissed = store.dismissed()
        // A day the user removed comes back only with new files; the old ones stay as extra options after them.
        val byDate = groupByDate(found, zone(), from, to).mapNotNull { (date, entries) ->
            val seen = if (run.includeRejected) emptySet() else dismissed[dismissKey(run.projectId, date)]?.toSet().orEmpty()
            val fresh = entries.filter { !isDismissed(it.uri, it.name, seen) }
            if (fresh.isEmpty()) null else date to (fresh to entries.filter { isDismissed(it.uri, it.name, seen) })
        }.toMap()
        // Days that already have a clip: remember the new items as alternatives (badge in the calendar), never replace.
        val gapSet = gaps.toSet()
        groupByDate(found, zone(), from, to).forEach { (date, entries) ->
            if (date in gapSet) return@forEach
            val day = days.getDay(run.projectId, date) ?: return@forEach
            if (day.draft || day.isAuto()) return@forEach
            store.addAlternatives(run.projectId, date, candidatesOf(entries))
        }
        val work = gaps.filter { it in byDate }
        val total = known + work.size
        store.update(run.id) { it.copy(done = known, total = total, scanned = found.size) }
        val format = projects.projects.first().firstOrNull { it.id == run.projectId }?.format ?: FilmFormat.CINEMA
        var done = known
        for (date in work) {
            if (store.current()?.let { it.id == run.id && !it.finished } != true) return
            if (days.getDay(run.projectId, date) != null) continue
            store.update(run.id) { it.copy(current = date.toString()) }
            val (fresh, old) = byDate.getValue(date)
            val made = create(run, date, fresh, format, old)
            // A rejected item picked again is no longer rejected, so the launch cleanup keeps its draft.
            if (made != null && run.includeRejected) store.undismiss(run.projectId, date, dismissed[dismissKey(run.projectId, date)].orEmpty())
            if (made != null) done++ else failed++
            if (!store.update(run.id) {
                    it.copy(
                        done = done,
                        videos = it.videos + if (made == false) 1 else 0,
                        photos = it.photos + if (made == true) 1 else 0,
                        skipped = it.skipped + if (made == null) 1 else 0,
                        firstFilled = if (made != null) minOf(it.firstFilled ?: date.toString(), date.toString()) else it.firstFilled,
                        lastFilled = if (made != null) maxOf(it.lastFilled ?: date.toString(), date.toString()) else it.lastFilled,
                    )
                }
            ) return
        }
        store.update(run.id) { it.copy(finished = true, done = done, current = null) }
    }

    /** Owner: "show count!" — the gallery search reports how many items it has looked at so far (written ~3× a second). */
    private suspend fun scanGallery(run: ImportRun, from: LocalDate, to: LocalDate, sniff: Set<LocalDate>): List<MediaEntry> = coroutineScope {
        val counter = MutableStateFlow(0)
        val writer = launch {
            counter.collect { n ->
                if (n > 0) store.update(run.id) { it.copy(found = n) }
                delay(300)
            }
        }
        try {
            scanner.scan(from, to, zone(), sniff) { counter.value = it }.filter { !run.cameraOnly || it.camera }
        } finally {
            writer.cancel()
        }
    }

    private suspend fun scanFolder(run: ImportRun, from: LocalDate, to: LocalDate): List<MediaEntry> {
        val scanner = checkNotNull(folderScanner) { "No folder scanner" }
        val result = scanner.scan(checkNotNull(run.folder), from, to, zone()) { n -> store.update(run.id) { it.copy(found = n) } }
        val entries = cleanFolderEntries(result.entries)
        store.update(run.id) { it.copy(unreadable = result.unreadable, truncated = result.truncated, fileTimeDates = entries.count(MediaEntry::fileTime)) }
        return entries
    }

    /** Returns true when the day got a photo, false when it got a video, null when it failed. */
    private suspend fun create(run: ImportRun, date: LocalDate, entries: List<MediaEntry>, format: FilmFormat, seen: List<MediaEntry> = emptyList()): Boolean? {
        val pick = rank(entries).first()
        var tmp: String? = null
        return try {
            var startMs = 0L
            var isPhoto = !pick.video && !pick.motion
            val path = if (pick.motion) {
                val clip = gallery.extractMotionVideo(pick.uri)
                if (clip != null) {
                    tmp = clip.path
                    startMs = MotionPhoto.windowStartMs(clip.presentationUs, media.durationMs(clip.path))
                    clip.path
                } else {
                    isPhoto = true
                    gallery.copyToTmp(pick.uri)
                }
            } else {
                gallery.copyToTmp(pick.uri).also {
                    if (pick.video) startMs = middleWindowStartMs(media.durationMs(it))
                }
            }
            tmp = path
            days.saveClip(
                SaveClipRequest(
                    date = date, source = path, startMs = startMs, speed = 1f, cropY = null, format = format,
                    daySource = if (isPhoto) DaySource.PHOTO else DaySource.GALLERY,
                    takenAt = Instant.fromEpochMilliseconds(pick.takenAtMs), isPhoto = isPhoto,
                    projectId = run.projectId, draft = !run.keep, draftCandidates = encodeCandidates(candidatesOf(entries) + candidatesOf(seen)), importRunId = run.id,
                ),
            )
            isPhoto
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logDebug("AutoImport", "draft ${date} failed: $e")
            null
        } finally {
            tmp?.let(files::delete)
        }
    }

    /** Removes exactly the drafts this run created and still unconfirmed, then forgets the run. */
    suspend fun undoAll() {
        val run = store.current() ?: return
        store.clear()
        days.draftsOfRun(run.projectId, run.id).forEach { day ->
            files.delete(day.file)
            files.delete(day.thumb)
            days.deleteRow(day.projectId, day.date)
        }
    }
}
