package app.sliver.data.repo

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.backup.DriveTrashQueue
import app.sliver.data.backup.NoTombstones
import app.sliver.data.backup.TombstoneKind
import app.sliver.data.backup.TombstoneStore
import app.sliver.data.backup.stampDay
import app.sliver.data.backup.stampFilm
import app.sliver.data.db.Day
import app.sliver.data.export.ClipExporter
import app.sliver.data.light.ClipLightReader
import app.sliver.data.light.NO_LUMA
import app.sliver.data.export.ShortMode
import app.sliver.data.files.FileStore
import kotlin.time.Clock
import kotlin.time.Instant
import app.sliver.data.db.DayDao
import app.sliver.data.db.Film
import app.sliver.data.db.FilmDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

interface DayRepository {
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>>
    fun observeAll(): Flow<List<Day>>
    fun observeDay(date: LocalDate): Flow<Day?>
    fun observeCount(): Flow<Int>
    suspend fun getDay(date: LocalDate): Day?
    suspend fun getDay(projectId: String, date: LocalDate): Day?

    /** Days of every project; used to tell what a restore still has to fetch. */
    fun observeAllProjects(): Flow<List<Day>>
    suspend fun save(day: Day)
    suspend fun delete(date: LocalDate)

    /** Stores a day exactly as another phone made it (stamp included); no tombstone, no re-stamp. */
    suspend fun applyRemote(day: Day)

    /** Removes a row because another phone deleted it; no tombstone. */
    suspend fun removeRemote(projectId: String, date: LocalDate)

    /** Unconfirmed auto-imported days of the active project, by date. Every other observer here sees confirmed days only. */
    fun observeDrafts(): Flow<List<Day>>
    suspend fun draftsOfRun(projectId: String, runId: String): List<Day>

    /** Removes a draft row; its files are the caller's to delete. */
    suspend fun deleteRow(projectId: String, date: LocalDate)

    /** Exports 1 s of the source, then moves video + thumb into place and upserts the row. */
    suspend fun saveClip(request: SaveClipRequest): Day
}

data class SaveClipRequest(
    val date: LocalDate,
    val source: String,
    val startMs: Long,
    val speed: Float,
    val cropY: Float?,
    val format: FilmFormat,
    val daySource: DaySource,
    val takenAt: Instant,
    val shortMode: ShortMode = ShortMode.SLOW_TO_FIT,
    val look: LookParams = LookParams(),
    val motion: Motion = Motion.STILL,
    val keepAudio: Boolean = true,
    val color: Int = 0,
    val isPhoto: Boolean = daySource == DaySource.PHOTO,
    /** Null = the active project. */
    val projectId: String? = null,
    val draft: Boolean = false,
    val draftCandidates: String? = null,
    val importRunId: String? = null,
)

/** Best-effort place name for a media source that carries a location; null when absent or unresolvable. */
fun interface PlaceResolver {
    suspend fun resolve(source: String): String?
}

interface FilmRepository {
    fun observeAll(): Flow<List<Film>>

    /** Films of every project; used by backup. */
    fun observeAllProjects(): Flow<List<Film>>
    fun observeFilm(id: String): Flow<Film?>
    fun observeCount(): Flow<Int>
    suspend fun getFilm(id: String): Film?
    suspend fun save(film: Film)
    suspend fun delete(id: String)
    suspend fun applyRemote(film: Film)
    suspend fun removeRemote(id: String)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DayRepositoryImpl(
    private val dao: DayDao,
    private val projects: ProjectRepository,
    private val exporter: ClipExporter,
    private val files: FileStore,
    private val places: PlaceResolver = PlaceResolver { null },
    private val trash: DriveTrashQueue = DriveTrashQueue {},
    private val light: ClipLightReader = ClipLightReader { null },
    private val tombstones: TombstoneStore = NoTombstones,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : DayRepository {
    override suspend fun saveClip(request: SaveClipRequest): Day = with(request) {
        val stamp = takenAt.toEpochMilliseconds()
        val tmpVideo = files.newTmpPath(stamp)
        val tmpThumb = "$tmpVideo.jpg"
        try {
            val clip = if (isPhoto) exporter.photoToClip(source, motion, format, tmpVideo, tmpThumb, look)
            else exporter.exportClip(source, startMs, speed, cropY, format, tmpVideo, tmpThumb, shortMode, look)
            val place = places.resolve(source)
            val stats = light.read(tmpVideo)
            val projectId = request.projectId ?: projects.activeId()
            val file = files.dayPath(projectId, date)
            val thumb = files.thumbPath(projectId, date)
            check(files.move(tmpVideo, file) && files.move(tmpThumb, thumb)) { "Could not move export into place" }
            val day = Day(
                projectId = projectId, date = date, file = file, thumb = thumb, source = daySource, look = look, motion = motion,
                keepAudio = keepAudio, speed = speed, color = clip.color.takeIf { it != 0 } ?: color, cropY = cropY,
                takenAt = takenAt, createdAt = Clock.System.now(), place = place,
                driveFileId = dao.getByDate(projectId, date)?.driveFileId,
                luma = stats?.luma ?: NO_LUMA, tone = stats?.tone ?: 0,
                draft = draft, draftCandidates = draftCandidates, importRunId = importRunId,
            )
            val stamped = stampDay(dao.getByDate(projectId, date), day, nowMillis())
            dao.upsert(stamped)
            stamped
        } finally {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
        }
    }

    private val activeIds = projects.active.filterNotNull().map { it.id }.distinctUntilChanged()

    override fun observeRange(from: LocalDate, to: LocalDate) = activeIds.flatMapLatest { dao.observeRange(it, from, to) }
    override fun observeAll() = activeIds.flatMapLatest { dao.observeAll(it) }
    override fun observeDay(date: LocalDate) = activeIds.flatMapLatest { dao.observeByDate(it, date) }
    override fun observeCount() = activeIds.flatMapLatest { dao.observeCount(it) }
    override suspend fun getDay(date: LocalDate) = dao.getByDate(projects.activeId(), date)
    override suspend fun getDay(projectId: String, date: LocalDate) = dao.getByDate(projectId, date)
    override fun observeAllProjects() = dao.observeEverything()
    override fun observeDrafts() = activeIds.flatMapLatest { dao.observeDrafts(it) }
    override suspend fun draftsOfRun(projectId: String, runId: String) = dao.draftsOfRun(projectId, runId)
    override suspend fun deleteRow(projectId: String, date: LocalDate) = dao.delete(projectId, date)
    override suspend fun save(day: Day) {
        val projectId = day.projectId.ifEmpty { projects.activeId() }
        dao.upsert(stampDay(dao.getByDate(projectId, day.date), day.copy(projectId = projectId), nowMillis()))
    }

    override suspend fun delete(date: LocalDate) {
        val projectId = projects.activeId()
        val existing = dao.getByDate(projectId, date)
        existing?.driveFileId?.let { trash.enqueue(it) }
        dao.delete(projectId, date)
        if (existing != null && !existing.draft) tombstones.record(TombstoneKind.DAY, projectId, date.toString(), nowMillis())
    }

    override suspend fun applyRemote(day: Day) = dao.upsert(day)
    override suspend fun removeRemote(projectId: String, date: LocalDate) = dao.delete(projectId, date)
}

@OptIn(ExperimentalCoroutinesApi::class)
class FilmRepositoryImpl(
    private val dao: FilmDao,
    private val projects: ProjectRepository,
    private val tombstones: TombstoneStore = NoTombstones,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : FilmRepository {
    private val activeIds = projects.active.filterNotNull().map { it.id }.distinctUntilChanged()

    override fun observeAll() = activeIds.flatMapLatest { dao.observeAll(it) }
    override fun observeAllProjects() = dao.observeEverything()
    override fun observeFilm(id: String) = dao.observeById(id)
    override fun observeCount() = activeIds.flatMapLatest { dao.observeCount(it) }
    override suspend fun getFilm(id: String) = dao.getById(id)
    override suspend fun save(film: Film) {
        val full = film.copy(projectId = film.projectId.ifEmpty { projects.activeId() })
        dao.upsert(stampFilm(dao.getById(full.id), full, nowMillis()))
    }

    override suspend fun delete(id: String) {
        val film = dao.getById(id)
        dao.delete(id)
        if (film != null) tombstones.record(TombstoneKind.FILM, film.projectId, id, nowMillis())
    }

    override suspend fun applyRemote(film: Film) = dao.upsert(film)
    override suspend fun removeRemote(id: String) = dao.delete(id)
}
