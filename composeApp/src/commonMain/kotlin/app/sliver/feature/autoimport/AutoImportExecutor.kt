package app.sliver.feature.autoimport

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

/** Reads dated gallery items. Needs the full or partial media permission; with partial access only visible items come back. */
interface MediaScanner {
    /** Items taken in [from]..[to] (device [zone]); motion photos are only detected on [sniffDays]. */
    suspend fun scan(from: LocalDate, to: LocalDate, zone: TimeZone, sniffDays: Set<LocalDate>): List<MediaEntry>
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
) {
    /** Counts of days skipped because their item could not be read, for the summary. */
    var failed = 0
        private set

    /** Makes one draft per gap day that has gallery items. Resumable: days that already have a row are skipped. */
    suspend fun run() {
        val run = store.current()?.takeIf { !it.finished } ?: return
        failed = 0
        val from = LocalDate.parse(run.from)
        val to = LocalDate.fromEpochDays(today().toEpochDays() - 1)
        val known = days.draftsOfRun(run.projectId, run.id).size
        val gaps = datesBetween(from, to).filter { days.getDay(run.projectId, it) == null }
        store.update(run.id) { it.copy(gapDays = gaps.size, startedAt = it.startedAt.takeIf { s -> s > 0 } ?: nowMillis()) }
        val found = scanner.scan(from, to, zone(), gaps.toSet())
        val byDate = groupByDate(found, zone(), from, to)
        val work = gaps.filter { it in byDate }
        val total = known + work.size
        store.update(run.id) { it.copy(done = known, total = total, scanned = found.size) }
        val format = projects.projects.first().firstOrNull { it.id == run.projectId }?.format ?: FilmFormat.CINEMA
        var done = known
        for (date in work) {
            if (store.current()?.let { it.id == run.id && !it.finished } != true) return
            if (days.getDay(run.projectId, date) != null) continue
            store.update(run.id) { it.copy(current = date.toString()) }
            val made = create(run, date, byDate.getValue(date), format)
            if (made != null) done++ else failed++
            if (!store.update(run.id) {
                    it.copy(
                        done = done,
                        videos = it.videos + if (made == false) 1 else 0,
                        photos = it.photos + if (made == true) 1 else 0,
                        skipped = it.skipped + if (made == null) 1 else 0,
                    )
                }
            ) return
        }
        store.update(run.id) { it.copy(finished = true, done = done, current = null) }
    }

    /** Returns true when the day got a photo, false when it got a video, null when it failed. */
    private suspend fun create(run: ImportRun, date: LocalDate, entries: List<MediaEntry>, format: FilmFormat): Boolean? {
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
                    projectId = run.projectId, draft = !run.keep, draftCandidates = encodeCandidates(candidatesOf(entries)), importRunId = run.id,
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
