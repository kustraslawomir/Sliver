package app.sliver.data.backup

import app.sliver.data.db.Film
import app.sliver.data.db.Project
import app.sliver.data.settings.AppSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

const val MANIFEST_NAME = "manifest.json"

/** The single-file summary written into the .zip export; Drive no longer holds one (older backups did, and are still read). */
fun buildManifest(s: AppSettings, films: List<Film>, nowMillis: Long, project: Project? = null): String = buildJsonObject {
    put("version", 2)
    put("updatedAt", nowMillis)
    if (project != null) {
        put("projectId", project.id)
        putJsonObject("project") {
            put("name", project.name)
            put("format", project.format.name)
            put("startDate", project.startDate.toString())
        }
    }
    putJsonObject("settings") {
        put("defaultLook", s.defaultLook.name)
        put("newFilmFormat", s.newFilmFormat.name)
        put("dateStamp", s.dateStamp)
        put("saveFullRecordings", s.saveFullRecordings)
        putJsonObject("reminder") {
            put("enabled", s.reminder.enabled)
            put("time", s.reminder.time.toString())
            put("skipIfDone", s.reminder.skipIfDone)
            put("lastCall", s.reminder.lastCall)
            put("followGoldenHour", s.reminder.followGoldenHour)
            put("days", s.reminder.days.joinToString("") { if (it) "1" else "0" })
        }
        putJsonObject("notif") {
            put("filmReady", s.notif.filmReady)
            put("backupProblems", s.notif.backupProblems)
            put("onThisDay", s.notif.onThisDay)
        }
    }
    put(
        "films",
        JsonArray(
            films.map { f ->
                buildJsonObject {
                    put("id", f.id)
                    put("name", f.name)
                    put("format", f.format.name)
                    put("live", f.live)
                    put("audio", f.audio.name)
                    put("track", f.track?.let(::JsonPrimitive) ?: JsonNull)
                    put("cut", f.cut.name)
                    put("dateOn", f.dateOn)
                    put("look", f.look?.name?.let(::JsonPrimitive) ?: JsonNull)
                    put("lookIntensity", f.lookIntensity)
                    put("lookGrain", f.lookGrain?.let(::JsonPrimitive) ?: JsonNull)
                    put("mismatch", f.mismatch.name)
                }
            },
        ),
    )
}.toString()
