package app.sliver.data.backup

import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.LookParams
import app.sliver.data.LookPreset
import app.sliver.data.Mismatch
import app.sliver.data.Motion
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Project
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.SettingsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Writes a downloaded day video, regenerates its thumbnail and returns the average color (0 when unknown). */
fun interface RestoredDayWriter {
    suspend fun write(bytes: ByteArray, videoPath: String, thumbPath: String): Int
}

interface RestoreScheduler {
    fun start()
}

data class RestoreItem(
    val fileId: String,
    val date: LocalDate,
    val look: LookParams,
    val source: DaySource,
    val motion: Motion,
    /** Target project; null for a legacy backup, which restores into the active project. */
    val projectId: String? = null,
)

/** Project details written into a backup manifest. */
data class ManifestProject(val id: String, val name: String, val format: FilmFormat, val startDate: LocalDate)

/** A Drive folder holding one project's clips; the legacy flat layout is the root folder with [project] null. */
data class RestoreSource(val folderId: String, val project: ManifestProject?, val manifest: String?)

data class RestoreScan(val folderId: String, val items: List<RestoreItem>, val sources: List<RestoreSource> = emptyList())

fun parseManifestProject(json: String): ManifestProject? = runCatching {
    val root = Json.parseToJsonElement(json).jsonObject
    val id = root["projectId"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
    val p = root["project"]?.jsonObject
    ManifestProject(
        id = id,
        name = p?.get("name")?.jsonPrimitive?.contentOrNull ?: "Restored",
        format = p?.enum<FilmFormat>("format") ?: FilmFormat.CINEMA,
        startDate = p?.date("startDate") ?: LocalDate(1970, 1, 1),
    )
}.getOrNull()

fun parseRestoreItem(file: DriveFile): RestoreItem? {
    val p = file.appProperties
    val date = p["sliverDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val look = p["look"]?.let { runCatching { Json.decodeFromString(LookParams.serializer(), it) }.getOrNull() } ?: LookParams()
    return RestoreItem(
        file.id, date, look,
        p["source"]?.let { runCatching { DaySource.valueOf(it) }.getOrNull() } ?: DaySource.CAMERA,
        p["motion"]?.let { runCatching { Motion.valueOf(it) }.getOrNull() } ?: Motion.STILL,
    )
}

private inline fun <reified E : Enum<E>> JsonObject.enum(key: String): E? =
    this[key]?.jsonPrimitive?.contentOrNull?.let { v -> enumValues<E>().firstOrNull { it.name == v } }

private fun JsonObject.date(key: String): LocalDate? =
    this[key]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun parseManifestFilms(json: String): List<Film> = runCatching {
    val root = Json.parseToJsonElement(json).jsonObject
    (root["films"] as? JsonArray).orEmpty().mapNotNull { el ->
        val f = el.jsonObject
        Film(
            id = f["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
            name = f["name"]?.jsonPrimitive?.contentOrNull ?: "Film",
            format = f.enum<FilmFormat>("format") ?: FilmFormat.CINEMA,
            rangeType = f.enum<FilmRangeType>("rangeType") ?: FilmRangeType.CUSTOM,
            rangeFrom = f.date("rangeFrom"),
            rangeTo = f.date("rangeTo"),
            live = f["live"]?.jsonPrimitive?.boolean ?: false,
            audio = f.enum<FilmAudio>("audio") ?: FilmAudio.ORIGINAL,
            track = f["track"]?.jsonPrimitive?.contentOrNull,
            cut = f.enum<FilmCut>("cut") ?: FilmCut.CUT,
            dateOn = f["dateOn"]?.jsonPrimitive?.boolean ?: true,
            look = f.enum<LookPreset>("look"),
            mismatch = f.enum<Mismatch>("mismatch") ?: Mismatch.CENTER,
        )
    }
}.getOrDefault(emptyList())

/** Applies the manifest's preferences on top of [s]; account and onboarding state are never touched. */
fun applyManifestSettings(s: AppSettings, json: String): AppSettings = runCatching {
    val m = Json.parseToJsonElement(json).jsonObject["settings"]?.jsonObject ?: return@runCatching s
    val r = m["reminder"] as? JsonObject
    val n = m["notif"] as? JsonObject
    fun JsonObject?.flag(key: String, default: Boolean) = this?.get(key)?.jsonPrimitive?.boolean ?: default
    fun JsonObject?.time(key: String, default: LocalTime) =
        this?.get(key)?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default
    s.copy(
        defaultLook = m.enum<LookPreset>("defaultLook") ?: s.defaultLook,
        newFilmFormat = m.enum<FilmFormat>("newFilmFormat") ?: s.newFilmFormat,
        dateStamp = m.flag("dateStamp", s.dateStamp),
        saveFullRecordings = m.flag("saveFullRecordings", s.saveFullRecordings),
        reminder = s.reminder.copy(
            mode = r?.enum<ReminderMode>("mode") ?: s.reminder.mode,
            time = r.time("time", s.reminder.time),
            windowStart = r.time("windowStart", s.reminder.windowStart),
            windowEnd = r.time("windowEnd", s.reminder.windowEnd),
            days = r?.get("days")?.jsonPrimitive?.contentOrNull?.takeIf { it.length == 7 }?.map { it == '1' } ?: s.reminder.days,
        ),
        notif = s.notif.copy(
            runningLate = n.flag("runningLate", s.notif.runningLate),
            filmReady = n.flag("filmReady", s.notif.filmReady),
            backupProblems = n.flag("backupProblems", s.notif.backupProblems),
            onThisDay = n.flag("onThisDay", s.notif.onThisDay),
        ),
    )
}.getOrDefault(s)

class RestoreEngine(
    private val drive: DriveClient,
    private val days: DayRepository,
    private val films: FilmRepository,
    private val settings: SettingsRepository,
    private val files: FileStore,
    private val writer: RestoredDayWriter,
    private val projects: ProjectRepository,
) {
    private val lock = Mutex()
    private val _progress = MutableStateFlow<BackupProgress?>(null)
    val progress: StateFlow<BackupProgress?> = _progress

    /** Lists what Drive holds: the legacy flat root plus one subfolder per project. Items are oldest first. */
    suspend fun scan(): RestoreScan {
        val account = settings.settings.first().account
        val root = drive.ensureFolder(account.driveFolderId)
        if (root != account.driveFolderId) {
            settings.update { s -> s.copy(account = s.account.copy(driveFolderId = root)) }
        }
        val activeId = projects.activeId()
        val sources = mutableListOf(RestoreSource(root, null, drive.readManifest(root)))
        for (folder in drive.listFolders(root)) {
            val manifest = drive.readManifest(folder.id) ?: continue
            sources += RestoreSource(folder.id, parseManifestProject(manifest), manifest)
        }
        val items = sources.flatMap { source ->
            val projectId = source.project?.id ?: activeId
            drive.list(source.folderId).mapNotNull(::parseRestoreItem).map { it.copy(projectId = projectId) }
        }
        return RestoreScan(root, items.sortedBy { it.date }, sources)
    }

    /** Downloads every second Drive has and this phone lacks. A day captured here always wins. Null on success. */
    suspend fun run(): BackupError? = lock.withLock {
        try {
            _progress.value = BackupProgress(0, 0)
            val scan = scan()
            val emptyBefore = days.observeCount().first() == 0
            for (project in scan.sources.mapNotNull { it.project }.distinctBy { it.id }) ensureProject(project)
            val todo = scan.items.filter { days.getDay(it.projectId ?: projects.activeId(), it.date) == null }
            var done = 0
            _progress.value = BackupProgress(0, todo.size)
            for (item in todo) {
                restoreDay(item)
                _progress.value = BackupProgress(++done, todo.size)
            }
            for (source in scan.sources) {
                val manifest = source.manifest ?: continue
                val projectId = source.project?.id ?: projects.activeId()
                parseManifestFilms(manifest).filter { films.getFilm(it.id) == null }.forEach { films.save(it.copy(projectId = projectId)) }
                settings.update { applyManifestSettings(it, manifest) }
            }
            if (emptyBefore) scan.items.firstOrNull { it.projectId != null }?.projectId?.let { projects.setActive(it) }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            (e as? DriveException)?.error ?: BackupError.OTHER
        } finally {
            _progress.value = null
        }
    }

    private suspend fun ensureProject(p: ManifestProject) {
        if (projects.projects.first().any { it.id == p.id }) return
        projects.restore(Project(p.id, p.name, p.format, p.startDate, createdAt = Clock.System.now()))
    }

    private suspend fun restoreDay(item: RestoreItem) {
        val bytes = drive.download(item.fileId)
        val tmpVideo = files.newTmpPath(Clock.System.now().toEpochMilliseconds())
        val tmpThumb = "$tmpVideo.jpg"
        val projectId = item.projectId ?: projects.activeId()
        val file = files.dayPath(projectId, item.date)
        val thumb = files.thumbPath(projectId, item.date)
        val color = try {
            writer.write(bytes, tmpVideo, tmpThumb)
        } catch (e: Exception) {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
            throw e
        }
        if (days.getDay(projectId, item.date) != null || !(files.move(tmpVideo, file) && files.move(tmpThumb, thumb))) {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
            return
        }
        val noon = LocalDateTime(item.date, LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
        days.save(
            Day(
                projectId = projectId, date = item.date, file = file, thumb = thumb, source = item.source, look = item.look, motion = item.motion,
                color = color, takenAt = noon, createdAt = Clock.System.now(),
                driveFileId = item.fileId, backup = BackupState.DONE,
            ),
        )
    }
}
