package app.sliver.data.backup

import app.sliver.data.BackupState
import app.sliver.data.LookParams
import app.sliver.data.db.Day
import app.sliver.data.db.Film
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.Json

fun interface DayFileReader {
    fun read(path: String): ByteArray?
}

/** Remembers Drive files whose day was removed locally, so the next backup trashes them. */
fun interface DriveTrashQueue {
    suspend fun enqueue(fileId: String)
}

class SettingsTrashQueue(private val settings: SettingsRepository) : DriveTrashQueue {
    override suspend fun enqueue(fileId: String) {
        settings.update { s -> s.copy(account = s.account.copy(pendingTrash = (s.account.pendingTrash + fileId).distinct())) }
    }
}

data class BackupProgress(val done: Int, val total: Int)

class BackupEngine(
    private val drive: DriveClient,
    private val days: DayRepository,
    private val films: FilmRepository,
    private val settings: SettingsRepository,
    private val reader: DayFileReader,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val lock = Mutex()
    private val _progress = MutableStateFlow<BackupProgress?>(null)
    val progress: StateFlow<BackupProgress?> = _progress

    /** Uploads every day that still needs it. Returns null on success, otherwise the error that stopped the run. */
    suspend fun run(): BackupError? = lock.withLock {
        try {
            _progress.value = BackupProgress(0, 0)
            sync()
            settings.update { s -> s.copy(account = s.account.copy(backupError = null, failingSince = null, problemNotified = false)) }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = (e as? DriveException)?.error ?: BackupError.OTHER
            settings.update { s ->
                s.copy(account = s.account.copy(backupError = error, failingSince = s.account.failingSince ?: nowMillis()))
            }
            error
        } finally {
            _progress.value = null
        }
    }

    private suspend fun sync() {
        val account = settings.settings.first().account
        val folder = drive.ensureFolder(account.driveFolderId)
        if (folder != account.driveFolderId) {
            settings.update { s -> s.copy(account = s.account.copy(driveFolderId = folder)) }
        }
        for (id in account.pendingTrash) {
            try {
                drive.trash(id)
            } catch (e: DriveException) {
                if (e.error != BackupError.OTHER) throw e
            }
            settings.update { s -> s.copy(account = s.account.copy(pendingTrash = s.account.pendingTrash - id)) }
        }

        var done = 0
        var retryFailed = true
        while (true) {
            val todo = days.observeAll().first().filter {
                it.backup == BackupState.PENDING || (retryFailed && it.backup == BackupState.FAILED)
            }
            retryFailed = false
            if (todo.isEmpty()) break
            val total = done + todo.size
            _progress.value = BackupProgress(done, total)
            for (day in todo) {
                upload(folder, day)
                _progress.value = BackupProgress(++done, total)
            }
        }
        drive.writeManifest(folder, buildManifest(settings.settings.first(), films.observeAll().first(), nowMillis()))
    }

    private suspend fun upload(folder: String, day: Day) {
        val bytes = reader.read(day.file)
        if (bytes == null) {
            days.save(day.copy(backup = BackupState.FAILED))
            return
        }
        val props = mapOf(
            "sliverDate" to day.date.toString(),
            "look" to Json.encodeToString(LookParams.serializer(), day.look),
            "source" to day.source.name,
            "motion" to day.motion.name,
        )
        val file = drive.upload(folder, "${day.date}.mp4", props, bytes, day.driveFileId)
        val current = days.getDay(day.date)
        when {
            current == null -> drive.trash(file.id)
            current.createdAt != day.createdAt -> days.save(current.copy(driveFileId = file.id))
            else -> days.save(current.copy(driveFileId = file.id, backup = BackupState.DONE))
        }
    }
}

fun buildManifest(s: AppSettings, films: List<Film>, nowMillis: Long): String = buildJsonObject {
    put("version", 1)
    put("updatedAt", nowMillis)
    putJsonObject("settings") {
        put("defaultLook", s.defaultLook.name)
        put("newFilmFormat", s.newFilmFormat.name)
        put("dateStamp", s.dateStamp)
        put("saveFullRecordings", s.saveFullRecordings)
        putJsonObject("reminder") {
            put("mode", s.reminder.mode.name)
            put("time", s.reminder.time.toString())
            put("windowStart", s.reminder.windowStart.toString())
            put("windowEnd", s.reminder.windowEnd.toString())
            put("days", s.reminder.days.joinToString("") { if (it) "1" else "0" })
        }
        putJsonObject("notif") {
            put("runningLate", s.notif.runningLate)
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
                    put("rangeType", f.rangeType.name)
                    put("rangeFrom", f.rangeFrom?.toString()?.let(::JsonPrimitive) ?: JsonNull)
                    put("rangeTo", f.rangeTo?.toString()?.let(::JsonPrimitive) ?: JsonNull)
                    put("live", f.live)
                    put("audio", f.audio.name)
                    put("track", f.track?.let(::JsonPrimitive) ?: JsonNull)
                    put("cut", f.cut.name)
                    put("dateOn", f.dateOn)
                    put("look", f.look?.name?.let(::JsonPrimitive) ?: JsonNull)
                    put("mismatch", f.mismatch.name)
                }
            },
        ),
    )
}.toString()
