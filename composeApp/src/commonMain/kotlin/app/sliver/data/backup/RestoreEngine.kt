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
import kotlinx.serialization.json.intOrNull
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

data class RestoreScan(val folderId: String, val items: List<RestoreItem>)

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

internal inline fun <reified E : Enum<E>> JsonObject.enum(key: String): E? =
    this[key]?.jsonPrimitive?.contentOrNull?.let { v -> enumValues<E>().firstOrNull { it.name == v } }

internal fun JsonObject.date(key: String): LocalDate? =
    this[key]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun parseManifestFilms(json: String): List<Film> = runCatching {
    val root = Json.parseToJsonElement(json).jsonObject
    (root["films"] as? JsonArray).orEmpty().mapNotNull { parseFilmObject(it.jsonObject) }
}.getOrDefault(emptyList())

fun parseFilmObject(f: JsonObject): Film? = Film(
    id = f["id"]?.jsonPrimitive?.contentOrNull ?: return null,
    name = f["name"]?.jsonPrimitive?.contentOrNull ?: "Film",
    format = f.enum<FilmFormat>("format") ?: FilmFormat.CINEMA,
    rangeType = f.enum<FilmRangeType>("rangeType") ?: FilmRangeType.YEAR,
    rangeFrom = f.date("rangeFrom"),
    rangeTo = f.date("rangeTo"),
    live = f["live"]?.jsonPrimitive?.boolean ?: false,
    audio = f.enum<FilmAudio>("audio") ?: FilmAudio.ORIGINAL,
    track = f["track"]?.jsonPrimitive?.contentOrNull,
    cut = f.enum<FilmCut>("cut") ?: FilmCut.CUT,
    dateOn = f["dateOn"]?.jsonPrimitive?.boolean ?: true,
    look = f.enum<LookPreset>("look"),
    lookIntensity = f["lookIntensity"]?.jsonPrimitive?.intOrNull ?: 100,
    lookGrain = f["lookGrain"]?.jsonPrimitive?.intOrNull,
    mismatch = f.enum<Mismatch>("mismatch") ?: Mismatch.CENTER,
    stampStyle = f.enum<app.sliver.data.StampStyle>("stampStyle"),
    musicVolume = f["musicVolume"]?.jsonPrimitive?.intOrNull,
)

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
            enabled = r.flag("enabled", s.reminder.enabled),
            time = r.time("time", s.reminder.time),
            skipIfDone = r.flag("skipIfDone", s.reminder.skipIfDone),
            lastCall = r.flag("lastCall", s.reminder.lastCall),
            followGoldenHour = r.flag("followGoldenHour", s.reminder.followGoldenHour),
            days = r?.get("days")?.jsonPrimitive?.contentOrNull?.takeIf { it.length == 7 }?.map { it == '1' } ?: s.reminder.days,
        ),
        notif = s.notif.copy(
            filmReady = n.flag("filmReady", s.notif.filmReady),
            backupProblems = n.flag("backupProblems", s.notif.backupProblems),
            onThisDay = n.flag("onThisDay", s.notif.onThisDay),
        ),
    )
}.getOrDefault(s)

/** Restore on a fresh install is simply the first pull of a sync. */
class RestoreEngine(
    private val sync: SyncEngine,
    private val days: DayRepository,
    private val projects: ProjectRepository,
) {
    val progress: StateFlow<BackupProgress?> = sync.progress

    suspend fun scan(): RestoreScan = sync.scanRemote()

    /** Null on success. A day captured here always wins over an older one on Drive. */
    suspend fun run(): BackupError? {
        val emptyBefore = days.observeCount().first() == 0
        val first = runCatching { sync.scanRemote().items.firstOrNull { it.projectId != null } }.getOrNull()
        val error = sync.run()
        if (error == null && emptyBefore) first?.projectId?.let { projects.setActive(it) }
        return error
    }
}

