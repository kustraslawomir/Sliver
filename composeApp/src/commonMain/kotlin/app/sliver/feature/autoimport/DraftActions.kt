package app.sliver.feature.autoimport

import app.sliver.core.AppScope
import app.sliver.core.navigation.Route
import app.sliver.core.ui.UndoToasts
import app.sliver.data.db.Day
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.films.ensureDefaultFilm
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.loadIntoSession
import app.sliver.feature.onboarding.FirstClipTimer
import app.sliver.feature.trim.UNDO_WINDOW_MS
import app.sliver.feature.trim.isTmp
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The draft shown in the editor with the gallery items of its day; [selectedUri] is the one currently loaded. */
class DraftContext(val day: Day, val candidates: List<DraftCandidate>, val selectedUri: String?, val chain: Boolean = true)

/** Opening, confirming and removing days that wait for a look: drafts and auto-filled days. */
class DraftActions(
    private val days: DayRepository,
    private val films: FilmRepository,
    private val files: FileStore,
    private val gallery: GalleryProvider,
    private val store: AutoImportStore? = null,
) {
    /** Drafts and auto-filled days of the active project, by date. */
    val drafts: Flow<List<Day>> get() = combine(days.observeDrafts(), days.observeAll(), ::toCheck)

    /** Loads [day] (or one of its candidates) into the editor session; returns the editor route, null if the item can't be read. */
    suspend fun open(day: Day, uri: String? = null, asVideo: Boolean? = null, chain: Boolean = true): Route? {
        // Days filled before the new order still show videos → moving photos → stills.
        val candidates = decodeCandidates(day.draftCandidates).sortedBy { c ->
            when {
                c.motion || c.live -> 1
                c.video -> 0
                else -> 2
            }
        }
        // A picked item must load; otherwise the first readable item of the day is used (no false "couldn't read").
        val order = if (uri != null) listOf(uri) else candidates.map { it.uri }
        for (target in order) {
            val route = loadIntoSession(gallery, files, target, day.date, asVideo) ?: continue
            CaptureSession.draft = DraftContext(day, candidates, target, chain)
            return route
        }
        return null
    }

    suspend fun first(): Day? = drafts.first().firstOrNull()
    suspend fun next(date: kotlinx.datetime.LocalDate): Day? = nextDraft(drafts.first(), date) { it.date }
    suspend fun previous(date: kotlinx.datetime.LocalDate): Day? = previousDraft(drafts.first(), date) { it.date }

    /** Saves the editor's choice over the draft and makes it a normal day. */
    suspend fun keep(draft: DraftContext, request: SaveClipRequest): Day {
        val day = days.saveClip(
            request.copy(projectId = draft.day.projectId, draft = false, draftCandidates = null, importRunId = null),
        )
        if (isTmp(request.source)) files.delete(request.source)
        store?.clearAlternatives(draft.day.projectId, draft.day.date)
        FirstClipTimer.clipSaved()
        if (days.observeAll().first().size == 1) runCatching { films.ensureDefaultFilm(day.date, request.format) }
        return day
    }

    /** Keeps every draft and auto-filled day of the active project as it is; one undo restores them. */
    suspend fun keepAll(): Int = keepDays(drafts.first())

    /** Marks [list] as reviewed (default pick, default 1 s). */
    suspend fun keepDays(list: List<Day>): Int {
        list.forEach { days.save(it.copy(draft = false, draftCandidates = null, importRunId = null)) }
        if (list.isNotEmpty()) {
            FirstClipTimer.clipSaved()
            UndoToasts.show(keptLabel(list.size)) { AppScope.launch { list.forEach { days.save(it) } } }
        }
        return list.size
    }

    /** Removes every auto-filled day that was not reviewed; drafts and camera days stay. One undo brings them back. */
    suspend fun removeUnchecked(): Int {
        val list = autoDays(days.observeAll().first())
        remove(list, removedLabel(list.size))
        return list.size
    }

    /** Owner: a single day is removed after a confirm, with no undo bar. */
    suspend fun skip(day: Day) = remove(listOf(day), null)

    /**
     * Deletes the days the way a user delete does (confirmed days leave a tombstone for sync); the files wait in tmp for
     * [UNDO_WINDOW_MS] so Undo can put everything back.
     */
    suspend fun remove(list: List<Day>, message: String?) {
        if (list.isEmpty()) return
        val stamp = Clock.System.now().toEpochMilliseconds()
        val parked = list.mapIndexed { i, day ->
            val video = files.newTmpPath(stamp + i)
            val thumb = "$video.jpg"
            val ok = files.move(day.file, video) && files.move(day.thumb, thumb)
            if (day.draft) days.deleteRow(day.projectId, day.date) else days.delete(day.date)
            Triple(day, video, thumb) to ok
        }
        val remembered = list.filter { it.draftCandidates != null || it.importRunId != null }
            .map { it to decodeCandidates(it.draftCandidates).flatMap(::dismissTokens) }
        remembered.forEach { (d, uris) -> store?.dismiss(d.projectId, d.date, uris) }
        if (message == null) {
            parked.forEach { (p, _) ->
                files.delete(p.second)
                files.delete(p.third)
            }
            return
        }
        val timer = AppScope.launch {
            delay(UNDO_WINDOW_MS)
            parked.forEach { (p, _) ->
                files.delete(p.second)
                files.delete(p.third)
            }
        }
        UndoToasts.show(message) {
            timer.cancel()
            AppScope.launch {
                remembered.forEach { (d, uris) -> store?.undismiss(d.projectId, d.date, uris) }
                parked.forEach { (p, ok) ->
                    val (day, video, thumb) = p
                    if (ok) {
                        files.move(video, day.file)
                        files.move(thumb, day.thumb)
                    }
                    days.save(day)
                }
            }
        }
    }
}
