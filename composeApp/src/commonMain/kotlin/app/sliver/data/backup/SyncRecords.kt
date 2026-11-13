package app.sliver.data.backup

import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.db.Film
import app.sliver.data.db.Project
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

const val PROJECTS_DIR = "projects"
const val FILMS_DIR = "films"
const val DELETED_DIR = "deleted"
const val REPLACED_DIR = "replaced"
const val DAY_MS = 24L * 60 * 60 * 1000
const val TOMBSTONE_KEEP_MS = 90 * DAY_MS
const val REPLACED_KEEP_MS = 30 * DAY_MS
const val PHONE_WINDOW_MS = 30 * DAY_MS

/** Who wrote a Drive record and when; a missing stamp (older backups) counts as the oldest possible. */
data class Stamp(val at: Long, val by: String) {
    /** What a record written by an older app (no device id) is stored as locally, so it never counts as a local edit. */
    fun asStored() = if (by.isEmpty()) copy(by = LEGACY_DEVICE) else this
}

const val LEGACY_DEVICE = "legacy"

val DriveFile.stamp get() = Stamp(appProperties["upd"]?.toLongOrNull() ?: 0L, appProperties["dev"].orEmpty())

fun compareVersions(a: Stamp, b: Stamp) = compareVersions(a.at, a.by, b.at, b.by)

fun recordProps(kind: String, at: Long, by: String) = mapOf("kind" to kind, "upd" to at.toString(), "dev" to by)

/** A clip file on Drive: `<date>.mp4` carrying the day's settings in appProperties. */
data class RemoteDay(val file: DriveFile, val date: LocalDate, val stamp: Stamp, val hash: String?)

fun parseRemoteDay(file: DriveFile): RemoteDay? {
    val date = file.appProperties["sliverDate"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    if (file.appProperties["kind"] == "tomb" || !file.name.endsWith(".mp4")) return null
    return RemoteDay(file, date, file.stamp, file.appProperties["hash"])
}

fun dayProps(day: app.sliver.data.db.Day, by: String, hash: String): Map<String, String> = buildMap {
    put("kind", "day")
    put("sliverDate", day.date.toString())
    put("look", Json.encodeToString(LookParams.serializer(), day.look))
    put("source", day.source.name)
    put("motion", day.motion.name)
    put("audio", if (day.keepAudio) "1" else "0")
    put("speed", day.speed.toString())
    day.cropY?.let { put("cropY", it.toString()) }
    day.cropX?.let { put("cropX", it.toString()) }
    put("taken", day.takenAt.toEpochMilliseconds().toString())
    put("luma", day.luma.toString())
    put("tone", day.tone.toString())
    put("upd", day.updatedAt.toString())
    put("dev", by)
    put("hash", hash)
}

fun projectJson(p: Project): String = buildJsonObject {
    put("id", p.id)
    put("name", p.name)
    put("format", p.format.name)
    put("startDate", p.startDate.toString())
    put("endDate", p.endDate?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("archived", p.archived)
    put("colorSeed", p.colorSeed)
    put("createdAt", p.createdAt.toEpochMilliseconds())
}.toString()

fun parseProjectJson(text: String, stamp: Stamp): Project? = runCatching {
    val o = Json.parseToJsonElement(text).jsonObject
    Project(
        id = o.getValue("id").jsonPrimitive.content,
        name = o["name"]?.jsonPrimitive?.contentOrNull ?: "Project",
        format = o.enum<FilmFormat>("format") ?: FilmFormat.CINEMA,
        startDate = o.date("startDate") ?: LocalDate(1970, 1, 1),
        endDate = o.date("endDate"),
        createdAt = Instant.fromEpochMilliseconds(o["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L),
        archived = o["archived"]?.jsonPrimitive?.boolean ?: false,
        colorSeed = o["colorSeed"]?.jsonPrimitive?.intOrNull ?: 0,
        updatedAt = stamp.at,
        updatedBy = stamp.by,
    )
}.getOrNull()

/** The film definition only: renders are never uploaded, each phone renders locally. */
fun filmJson(f: Film): String = buildJsonObject {
    put("id", f.id)
    put("name", f.name)
    put("format", f.format.name)
    put("rangeType", f.rangeType.name)
    put("rangeFrom", f.rangeFrom?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("rangeTo", f.rangeTo?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("live", f.live)
    put("audio", f.audio.name)
    put("track", f.track?.let(::JsonPrimitive) ?: JsonNull)
    put("cut", f.cut.name)
    put("dateOn", f.dateOn)
    put("look", f.look?.name?.let(::JsonPrimitive) ?: JsonNull)
    put("lookIntensity", f.lookIntensity)
    put("lookGrain", f.lookGrain?.let(::JsonPrimitive) ?: JsonNull)
    put("mismatch", f.mismatch.name)
}.toString()

fun parseFilmJson(text: String, projectId: String, stamp: Stamp): Film? = runCatching {
    parseFilmObject(Json.parseToJsonElement(text).jsonObject)?.copy(projectId = projectId, updatedAt = stamp.at, updatedBy = stamp.by)
}.getOrNull()

fun parseDaySource(s: String?) = s?.let { runCatching { DaySource.valueOf(it) }.getOrNull() } ?: DaySource.CAMERA
fun parseMotion(s: String?) = s?.let { runCatching { Motion.valueOf(it) }.getOrNull() } ?: Motion.STILL
