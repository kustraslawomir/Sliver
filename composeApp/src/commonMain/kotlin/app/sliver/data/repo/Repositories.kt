package app.sliver.data.repo

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.db.Day
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.ShortMode
import app.sliver.data.files.FileStore
import kotlin.time.Clock
import kotlin.time.Instant
import app.sliver.data.db.DayDao
import app.sliver.data.db.Film
import app.sliver.data.db.FilmDao
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface DayRepository {
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>>
    fun observeAll(): Flow<List<Day>>
    fun observeDay(date: LocalDate): Flow<Day?>
    fun observeCount(): Flow<Int>
    suspend fun getDay(date: LocalDate): Day?
    suspend fun save(day: Day)
    suspend fun delete(date: LocalDate)

    /** Exports 1 s of the source, then moves video + thumb into place and upserts the row. */
    suspend fun saveSecond(request: SaveSecondRequest): Day
}

data class SaveSecondRequest(
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
    fun observeFilm(id: String): Flow<Film?>
    fun observeCount(): Flow<Int>
    suspend fun getFilm(id: String): Film?
    suspend fun save(film: Film)
    suspend fun delete(id: String)
}

class DayRepositoryImpl(
    private val dao: DayDao,
    private val exporter: ClipExporter,
    private val files: FileStore,
    private val places: PlaceResolver = PlaceResolver { null },
) : DayRepository {
    override suspend fun saveSecond(request: SaveSecondRequest): Day = with(request) {
        val stamp = takenAt.toEpochMilliseconds()
        val tmpVideo = files.newTmpPath(stamp)
        val tmpThumb = "$tmpVideo.jpg"
        try {
            val clip = if (isPhoto) exporter.photoToSecond(source, motion, format, tmpVideo, tmpThumb, look)
            else exporter.exportSecond(source, startMs, speed, cropY, format, tmpVideo, tmpThumb, shortMode, look)
            val place = places.resolve(source)
            val file = files.dayPath(date)
            val thumb = files.thumbPath(date)
            check(files.move(tmpVideo, file) && files.move(tmpThumb, thumb)) { "Could not move export into place" }
            val day = Day(
                date = date, file = file, thumb = thumb, source = daySource, look = look, motion = motion,
                keepAudio = keepAudio, speed = speed, color = clip.color.takeIf { it != 0 } ?: color, cropY = cropY,
                takenAt = takenAt, createdAt = Clock.System.now(), place = place,
            )
            dao.upsert(day)
            day
        } finally {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
        }
    }

    override fun observeRange(from: LocalDate, to: LocalDate) = dao.observeRange(from, to)
    override fun observeAll() = dao.observeAll()
    override fun observeDay(date: LocalDate) = dao.observeByDate(date)
    override fun observeCount() = dao.observeCount()
    override suspend fun getDay(date: LocalDate) = dao.getByDate(date)
    override suspend fun save(day: Day) = dao.upsert(day)
    override suspend fun delete(date: LocalDate) = dao.delete(date)
}

class FilmRepositoryImpl(private val dao: FilmDao) : FilmRepository {
    override fun observeAll() = dao.observeAll()
    override fun observeFilm(id: String) = dao.observeById(id)
    override fun observeCount() = dao.observeCount()
    override suspend fun getFilm(id: String) = dao.getById(id)
    override suspend fun save(film: Film) = dao.upsert(film)
    override suspend fun delete(id: String) = dao.delete(id)
}
