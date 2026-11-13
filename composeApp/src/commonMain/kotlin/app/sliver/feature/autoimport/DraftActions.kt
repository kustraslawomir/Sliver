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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The draft shown in the editor with the gallery items of its day; [selectedUri] is the one currently loaded. */
class DraftContext(val day: Day, val candidates: List<DraftCandidate>, val selectedUri: String?)

/** Opening, confirming and skipping drafts. Drafts live in the calendar only until the user keeps them. */
class DraftActions(
    private val days: DayRepository,
    private val films: FilmRepository,
    private val files: FileStore,
    private val gallery: GalleryProvider,
) {
    val drafts: Flow<List<Day>> get() = days.observeDrafts()

    /** Loads [day] (or one of its candidates) into the editor session; returns the editor route, null if the item can't be read. */
    suspend fun open(day: Day, uri: String? = null, asVideo: Boolean? = null): Route? {
        val candidates = decodeCandidates(day.draftCandidates)
        val target = uri ?: candidates.firstOrNull()?.uri ?: return null
        val route = loadIntoSession(gallery, files, target, day.date, asVideo) ?: return null
        CaptureSession.draft = DraftContext(day, candidates, target)
        return route
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
        FirstClipTimer.clipSaved()
        if (days.observeAll().first().size == 1) runCatching { films.ensureDefaultFilm(day.date, request.format) }
        return day
    }

    /** Keeps every draft of the active project as it is (default pick, default 1 s); one undo restores them as drafts. */
    suspend fun keepAll(): Int {
        val all = drafts.first()
        all.forEach { days.save(it.copy(draft = false, draftCandidates = null, importRunId = null)) }
        if (all.isNotEmpty()) {
            FirstClipTimer.clipSaved()
            UndoToasts.show("${all.size} ${if (all.size == 1) "draft" else "drafts"} kept") {
                AppScope.launch { all.forEach { days.save(it) } }
            }
        }
        return all.size
    }

    /** Deletes the draft and leaves the day empty; the files wait in tmp for [UNDO_WINDOW_MS]. */
    suspend fun skip(day: Day) {
        val stamp = Clock.System.now().toEpochMilliseconds()
        val video = files.newTmpPath(stamp)
        val thumb = "$video.jpg"
        val parked = files.move(day.file, video) && files.move(day.thumb, thumb)
        days.deleteRow(day.projectId, day.date)
        val timer = AppScope.launch {
            delay(UNDO_WINDOW_MS)
            files.delete(video)
            files.delete(thumb)
        }
        UndoToasts.show("Draft skipped") {
            timer.cancel()
            AppScope.launch {
                if (parked) {
                    files.move(video, day.file)
                    files.move(thumb, day.thumb)
                }
                days.save(day)
            }
        }
    }
}
