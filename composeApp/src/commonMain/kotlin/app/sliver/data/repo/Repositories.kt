package app.sliver.data.repo

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.backup.DriveTrashQueue
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
            val projectId = projects.activeId()
            val file = files.dayPath(projectId, date)
            val thumb = files.thumbPath(projectId, date)
            check(files.move(tmpVideo, file) && files.move(tmpThumb, thumb)) { "Could not move export into place" }
            val day = Day(
                projectId = projectId, date = date, file = file, thumb = thumb, source = daySource, look = look, motion = motion,
                keepAudio = keepAudio, speed = speed, color = clip.color.takeIf { it != 0 } ?: color, cropY = cropY,
                takenAt = takenAt, createdAt = Clock.System.now(), place = place,
                driveFileId = dao.getByDate(projectId, date)?.driveFileId,
                luma = stats?.luma ?: NO_LUMA, tone = stats?.tone ?: 0,
            )
            dao.upsert(day)
            day
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
    override suspend fun save(day: Day) = dao.upsert(day.copy(projectId = day.projectId.ifEmpty { projects.activeId() }))
    override suspend fun delete(date: LocalDate) {
        val projectId = projects.activeId()
        dao.getByDate(projectId, date)?.driveFileId?.let { trash.enqueue(it) }
        dao.delete(projectId, date)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FilmRepositoryImpl(private val dao: FilmDao, private val projects: ProjectRepository) : FilmRepository {
    private val activeIds = projects.active.filterNotNull().map { it.id }.distinctUntilChanged()

    override fun observeAll() = activeIds.flatMapLatest { dao.observeAll(it) }
    override fun observeAllProjects() = dao.observeEverything()
    override fun observeFilm(id: String) = dao.observeById(id)
    override fun observeCount() = activeIds.flatMapLatest { dao.observeCount(it) }
    override suspend fun getFilm(id: String) = dao.getById(id)
    override suspend fun save(film: Film) = dao.upsert(film.copy(projectId = film.projectId.ifEmpty { projects.activeId() }))
    override suspend fun delete(id: String) = dao.delete(id)
}
