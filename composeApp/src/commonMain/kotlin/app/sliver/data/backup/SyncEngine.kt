package app.sliver.data.backup

import app.sliver.data.BackupState
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Project
import app.sliver.data.db.Tombstone
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.SettingsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import app.sliver.data.LookParams

fun interface DayFileReader {
    fun read(path: String): ByteArray?
}

/** Remembers Drive files whose day was removed locally, so the next sync trashes them. */
fun interface DriveTrashQueue {
    suspend fun enqueue(fileId: String)
}

class SettingsTrashQueue(private val settings: SettingsRepository) : DriveTrashQueue {
    override suspend fun enqueue(fileId: String) {
        settings.update { s -> s.copy(account = s.account.copy(pendingTrash = (s.account.pendingTrash + fileId).distinct())) }
    }
}

data class BackupProgress(val done: Int, val total: Int)

/**
 * Keeps one Drive folder in step with every phone signed in to the same account. Drive holds one file per record, so
 * nothing is ever overwritten wholesale: pull what is newer there, push what is newer here, last writer wins per record.
 */
@OptIn(ExperimentalUuidApi::class)
class SyncEngine(
    private val drive: DriveClient,
    private val days: DayRepository,
    private val films: FilmRepository,
    private val settings: SettingsRepository,
    private val reader: DayFileReader,
    private val projects: ProjectRepository,
    private val files: FileStore,
    private val writer: RestoredDayWriter,
    private val tombstones: TombstoneStore = NoTombstones,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val newId: () -> String = { Uuid.random().toString() },
) {
    private val lock = Mutex()
    private val _progress = MutableStateFlow<BackupProgress?>(null)
    val progress: StateFlow<BackupProgress?> = _progress

    private var done = 0
    private var total = 0

    /** Runs one pull-then-push cycle. Returns null on success, otherwise the error that stopped it. */
    suspend fun run(): BackupError? = lock.withLock {
        try {
            done = 0
            total = 0
            _progress.value = BackupProgress(0, 0)
            cycle()
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

    /** Lists the clips Drive holds, for the restore screen. */
    suspend fun scanRemote(): RestoreScan = lock.withLock {
        val root = rootFolder()
        val projectsDir = drive.ensureSubfolder(root, PROJECTS_DIR)
        migrateLegacy(root, projectsDir)
        val activeId = projects.activeId()
        val known = drive.list(projectsDir).filter { it.name.endsWith(".json") && !it.name.endsWith(DELETED_SUFFIX) }
            .map { it.name.removeSuffix(".json") }.toSet()
        val items = drive.listFolders(root).filter { it.name in known }.flatMap { folder ->
            drive.list(folder.id).mapNotNull(::parseRestoreItem).map { it.copy(projectId = folder.name) }
        } + drive.list(root).mapNotNull(::parseRestoreItem).map { it.copy(projectId = activeId) }
        RestoreScan(root, items.sortedBy { it.date })
    }

    private inner class Cycle(val me: String, val root: String, val startedAt: Long, val lastPushed: Long) {
        val seen = HashMap<String, Long>()

        fun note(stamp: Stamp) {
            if (stamp.by.isNotEmpty() && stamp.at > 0) seen[stamp.by] = maxOf(seen[stamp.by] ?: 0L, stamp.at)
        }

        fun Day.stamp() = Stamp(updatedAt, updatedBy.ifEmpty { me })
        fun Film.stamp() = Stamp(updatedAt, updatedBy.ifEmpty { me })
        fun Project.stamp() = Stamp(updatedAt, updatedBy.ifEmpty { me })

        /** A record this phone changed and Drive may not have yet. */
        fun Day.dirty() = updatedBy.isEmpty() && (backup != BackupState.DONE || updatedAt >= lastPushed)
        fun Film.dirty() = updatedBy.isEmpty() && updatedAt >= lastPushed
        fun Project.dirty() = updatedBy.isEmpty() && updatedAt >= lastPushed
    }

    private class ProjectDirs(val root: String, val films: String, val deleted: String, var replaced: String?)

    private suspend fun rootFolder(): String {
        val stored = settings.settings.first().account.driveFolderId
        val root = drive.ensureFolder(stored)
        if (root != stored) settings.update { s -> s.copy(account = s.account.copy(driveFolderId = root)) }
        return root
    }

    private suspend fun cycle() {
        val startedAt = nowMillis()
        var account = settings.settings.first().account
        val me = account.deviceId.ifEmpty {
            newId().also { id -> settings.update { s -> s.copy(account = s.account.copy(deviceId = id)) } }
        }
        val root = rootFolder()
        account = settings.settings.first().account
        val c = Cycle(me, root, startedAt, account.lastPushedAt)

        for (id in account.pendingTrash) {
            try {
                drive.trash(id)
            } catch (e: DriveException) {
                if (e.error != BackupError.OTHER) throw e
            }
            settings.update { s -> s.copy(account = s.account.copy(pendingTrash = s.account.pendingTrash - id)) }
        }

        val projectsDir = drive.ensureSubfolder(root, PROJECTS_DIR)
        migrateLegacy(root, projectsDir)

        val remoteProjects = RecordListing(drive.list(projectsDir))
        pullProjects(c, remoteProjects)
        val local = projects.projects.first()
        val dirs = local.associate { it.id to dirsOf(root, it.id) }
        for (p in local) pullProject(c, p, dirs.getValue(p.id))

        pushProjectDeletes(c, projectsDir, remoteProjects)
        pushProjects(c, projectsDir, remoteProjects, local)
        for (p in local) pushProject(c, p, dirs.getValue(p.id))
        collectGarbage(c, projectsDir, dirs.values)

        val phones = c.seen.count { it.value >= startedAt - PHONE_WINDOW_MS && it.key != me } + 1
        settings.update { s ->
            s.copy(account = s.account.copy(lastSyncAt = nowMillis(), lastPushedAt = startedAt, phones = phones))
        }
    }

    private suspend fun dirsOf(root: String, projectId: String): ProjectDirs {
        val folder = drive.ensureSubfolder(root, projectId)
        val subs = drive.listFolders(folder).associate { it.name to it.id }
        return ProjectDirs(
            folder,
            subs[FILMS_DIR] ?: drive.ensureSubfolder(folder, FILMS_DIR),
            subs[DELETED_DIR] ?: drive.ensureSubfolder(folder, DELETED_DIR),
            subs[REPLACED_DIR],
        )
    }

    private fun tick(planned: Int = 0, finished: Int = 0) {
        total += planned
        done += finished
        _progress.value = BackupProgress(done, total)
    }

    /** `<id>.json` records and `<id>.deleted.json` tombstones in one Drive folder. */
    private class RecordListing(files: List<DriveFile>) {
        val live = files.filter { it.name.endsWith(".json") && !it.name.endsWith(DELETED_SUFFIX) }.associateBy { it.name.removeSuffix(".json") }
        val gone = files.filter { it.name.endsWith(DELETED_SUFFIX) }.associateBy { it.name.removeSuffix(DELETED_SUFFIX) }
    }

    // ---- legacy manifest backups ----

    /**
     * Older backups kept one manifest.json per project folder (named after the project). Turns each into the
     * per-record layout: folder renamed to the project id, project and film records written, manifest trashed.
     */
    private suspend fun migrateLegacy(root: String, projectsDir: String) {
        val localProjects = projects.projects.first()
        val known = RecordListing(drive.list(projectsDir))
        // The oldest layout kept clips and one manifest straight in the root; its films belong to the active project.
        drive.list(root).firstOrNull { it.name == MANIFEST_NAME }?.let { file ->
            val manifest = drive.download(file.id).decodeToString()
            val filmsDir = drive.ensureSubfolder(drive.ensureSubfolder(root, projects.activeId()), FILMS_DIR)
            writeFilmRecords(filmsDir, manifest, manifestUpdatedAt(manifest))
            drive.trash(file.id)
        }
        for (folder in drive.listFolders(root)) {
            if (folder.name == PROJECTS_DIR) continue
            val manifestFile = drive.list(folder.id).firstOrNull { it.name == MANIFEST_NAME } ?: continue
            val manifest = drive.download(manifestFile.id).decodeToString()
            val fromManifest = parseManifestProject(manifest)
            val id = fromManifest?.id ?: localProjects.firstOrNull { it.name == folder.name }?.id ?: continue
            val updatedAt = manifestUpdatedAt(manifest)
            if (folder.name != id) drive.renameFolder(folder.id, id)
            if (id !in known.live) {
                val p = fromManifest?.let {
                    Project(id, it.name, it.format, it.startDate, createdAt = Instant.fromEpochMilliseconds(updatedAt))
                } ?: localProjects.first { it.id == id }
                drive.upload(projectsDir, "$id.json", recordProps("project", updatedAt, ""), projectJson(p).encodeToByteArray())
            }
            val filmsDir = drive.ensureSubfolder(folder.id, FILMS_DIR)
            writeFilmRecords(filmsDir, manifest, updatedAt)
            drive.trash(manifestFile.id)
        }
    }

    private suspend fun writeFilmRecords(filmsDir: String, manifest: String, updatedAt: Long) {
        val existing = drive.list(filmsDir).map { it.name }.toSet()
        for (film in parseManifestFilms(manifest)) {
            if ("${film.id}.json" in existing) continue
            drive.upload(filmsDir, "${film.id}.json", recordProps("film", updatedAt, ""), filmJson(film).encodeToByteArray())
        }
    }

    private fun manifestUpdatedAt(manifest: String): Long =
        runCatching { Json.parseToJsonElement(manifest).jsonObject["updatedAt"]?.jsonPrimitive?.longOrNull }.getOrNull() ?: 0L

    // ---- pull ----

    private suspend fun pullProjects(c: Cycle, remote: RecordListing) = with(c) {
        val localTombs = tombstones.all().filter { it.kind == TombstoneKind.PROJECT }.associateBy { it.projectId }
        val local = projects.projects.first().associateBy { it.id }
        for ((id, tomb) in remote.gone) {
            note(tomb.stamp)
            val l = local[id] ?: continue
            if (tomb.stamp.at > l.updatedAt) projects.removeRemote(id)
        }
        for ((id, file) in remote.live) {
            note(file.stamp)
            val gone = remote.gone[id]
            if (gone != null && gone.stamp.at >= file.stamp.at) continue
            val deletedHere = localTombs[id]
            if (deletedHere != null) {
                if (deletedHere.deletedAt >= file.stamp.at) continue
                tombstones.clear(TombstoneKind.PROJECT, id, "")
            }
            val l = local[id]
            if (l != null && compareVersions(file.stamp, l.stamp()) <= 0) continue
            tick(planned = 1)
            val project = parseProjectJson(drive.download(file.id).decodeToString(), file.stamp.asStored()) ?: continue
            projects.restore(project)
            tick(finished = 1)
        }
    }

    private suspend fun pullProject(c: Cycle, p: Project, dirs: ProjectDirs) = with(c) {
        val remote = remoteDays(c, p, dirs, includeFlat = true)
        val gone = tombDays(dirs)
        val localGone = tombstones.all().filter { it.kind == TombstoneKind.DAY && it.projectId == p.id }.associateBy { it.key }
        for (tomb in gone.values) note(tomb.stamp)

        for ((date, r) in remote) {
            note(r.stamp)
            val local = days.getDay(p.id, date)
            if (local?.draft == true) continue
            val tomb = gone[date]
            if (tomb != null && tomb.stamp.at > r.stamp.at) {
                if (local != null && tomb.stamp.at > local.updatedAt) removeLocalDay(local)
                continue
            }
            val deletedHere = localGone[date.toString()]
            if (deletedHere != null) {
                if (deletedHere.deletedAt >= r.stamp.at) continue
                tombstones.clear(TombstoneKind.DAY, p.id, date.toString())
            }
            if (local != null && compareVersions(r.stamp, local.stamp()) <= 0) continue
            applyDay(c, p, dirs, r, local)
        }
        for ((date, tomb) in gone) {
            if (date in remote) continue
            val local = days.getDay(p.id, date) ?: continue
            if (!local.draft && tomb.stamp.at > local.updatedAt) removeLocalDay(local)
        }
        pullFilms(c, p, dirs)
    }

    /** The newest file per date. Duplicates (two phones uploading at once) are parked in replaced/ and trashed. */
    private suspend fun tombDays(dirs: ProjectDirs): Map<LocalDate, DriveFile> =
        drive.list(dirs.deleted).mapNotNull { f -> runCatching { LocalDate.parse(f.name.removeSuffix(".json")) }.getOrNull()?.let { it to f } }.toMap()

    private suspend fun remoteDays(c: Cycle, p: Project, dirs: ProjectDirs, includeFlat: Boolean): MutableMap<LocalDate, RemoteDay> {
        val out = LinkedHashMap<LocalDate, RemoteDay>()
        for ((date, group) in drive.list(dirs.root).mapNotNull(::parseRemoteDay).groupBy { it.date }) {
            val sorted = group.sortedWith { a, b -> compareVersions(b.stamp, a.stamp) }
            out[date] = sorted.first()
            for (loser in sorted.drop(1)) {
                archiveRemote(dirs, loser)
                drive.trash(loser.file.id)
            }
        }
        if (includeFlat && p.id == projects.activeId()) {
            for (item in drive.list(c.root).mapNotNull(::parseRemoteDay)) out.getOrPut(item.date) { item }
        }
        return out
    }

    private suspend fun removeLocalDay(day: Day) {
        files.delete(day.file)
        files.delete(day.thumb)
        days.removeRemote(day.projectId, day.date)
    }

    private suspend fun applyDay(c: Cycle, p: Project, dirs: ProjectDirs, r: RemoteDay, local: Day?) = with(c) {
        tick(planned = 1)
        val bytes = drive.download(r.file.id)
        if (local != null && local.dirty()) {
            reader.read(local.file)?.let { archive(dirs, local.date, me, it, local.updatedAt) }
        }
        val tmpVideo = files.newTmpPath(nowMillis())
        val tmpThumb = "$tmpVideo.jpg"
        val color = try {
            writer.write(bytes, tmpVideo, tmpThumb)
        } catch (e: Exception) {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
            throw e
        }
        val file = files.dayPath(p.id, r.date)
        val thumb = files.thumbPath(p.id, r.date)
        if (!(files.move(tmpVideo, file) && files.move(tmpThumb, thumb))) {
            files.delete(tmpVideo)
            files.delete(tmpThumb)
            return@with
        }
        val props = r.file.appProperties
        val noon = LocalDateTime(r.date, LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
        days.applyRemote(
            Day(
                projectId = p.id,
                date = r.date,
                file = file,
                thumb = thumb,
                source = parseDaySource(props["source"]),
                look = props["look"]?.let { runCatching { Json.decodeFromString(LookParams.serializer(), it) }.getOrNull() } ?: LookParams(),
                motion = parseMotion(props["motion"]),
                keepAudio = props["audio"] != "0",
                speed = props["speed"]?.toFloatOrNull() ?: 1f,
                color = color,
                cropY = props["cropY"]?.toFloatOrNull(),
                cropX = props["cropX"]?.toFloatOrNull(),
                takenAt = props["taken"]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds) ?: noon,
                createdAt = Instant.fromEpochMilliseconds(nowMillis()),
                driveFileId = r.file.id,
                backup = BackupState.DONE,
                place = local?.place,
                luma = props["luma"]?.toFloatOrNull() ?: -1f,
                tone = props["tone"]?.toIntOrNull() ?: 0,
                updatedAt = r.stamp.at,
                updatedBy = r.stamp.by.ifEmpty { LEGACY_DEVICE },
            ),
        )
        if (local != null) {
            val key = "${p.id}|${r.date}"
            settings.update { s -> s.copy(account = s.account.copy(changedNotices = (s.account.changedNotices + key).distinct())) }
        }
        tick(finished = 1)
    }

    private suspend fun pullFilms(c: Cycle, p: Project, dirs: ProjectDirs) = with(c) {
        val remote = RecordListing(drive.list(dirs.films))
        val localTombs = tombstones.all().filter { it.kind == TombstoneKind.FILM && it.projectId == p.id }.associateBy { it.key }
        val local = films.observeAllProjects().first().filter { it.projectId == p.id }.associateBy { it.id }
        for ((id, tomb) in remote.gone) {
            note(tomb.stamp)
            val l = local[id] ?: continue
            if (tomb.stamp.at > l.updatedAt) {
                l.renderPath?.let(files::delete)
                films.removeRemote(id)
            }
        }
        for ((id, file) in remote.live) {
            note(file.stamp)
            val gone = remote.gone[id]
            if (gone != null && gone.stamp.at >= file.stamp.at) continue
            val deletedHere = localTombs[id]
            if (deletedHere != null) {
                if (deletedHere.deletedAt >= file.stamp.at) continue
                tombstones.clear(TombstoneKind.FILM, p.id, id)
            }
            val l = local[id]
            if (l != null && compareVersions(file.stamp, l.stamp()) <= 0) continue
            val film = parseFilmJson(drive.download(file.id).decodeToString(), p.id, file.stamp.asStored()) ?: continue
            val sameRender = l != null && l.sameDefinition(film)
            if (l != null && !sameRender) l.renderPath?.let(files::delete)
            films.applyRemote(if (sameRender) film.copy(renderPath = l?.renderPath, renderedAt = l?.renderedAt) else film)
        }
    }

    // ---- push ----

    private suspend fun pushProjectDeletes(c: Cycle, projectsDir: String, remote: RecordListing) = with(c) {
        for (t in tombstones.all().filter { it.kind == TombstoneKind.PROJECT }) {
            val live = remote.live[t.projectId]
            if (live != null && live.stamp.at > t.deletedAt) {
                tombstones.clear(t.kind, t.projectId, t.key)
                continue
            }
            drive.upload(projectsDir, "${t.projectId}$DELETED_SUFFIX", recordProps("tomb", t.deletedAt, me), "{}".encodeToByteArray(), remote.gone[t.projectId]?.id)
            live?.let { drive.trash(it.id) }
            drive.listFolders(root).firstOrNull { it.name == t.projectId }?.let { drive.trash(it.id) }
            tombstones.clear(t.kind, t.projectId, t.key)
        }
    }

    private suspend fun pushProjects(c: Cycle, projectsDir: String, remote: RecordListing, local: List<Project>) = with(c) {
        for (p in local) {
            val r = remote.live[p.id]
            val current = projects.projects.first().firstOrNull { it.id == p.id } ?: continue
            if (r != null && (!current.dirty() || compareVersions(current.stamp(), r.stamp) <= 0)) continue
            drive.upload(projectsDir, "${p.id}.json", recordProps("project", current.updatedAt, current.stamp().by), projectJson(current).encodeToByteArray(), r?.id)
            remote.gone[p.id]?.let { drive.trash(it.id) }
        }
    }

    private suspend fun pushProject(c: Cycle, p: Project, dirs: ProjectDirs) = with(c) {
        val remote = remoteDays(c, p, dirs, includeFlat = false)
        val gone = tombDays(dirs)
        val allTombs = tombstones.all()

        val todo = days.observeAllProjects().first().filter { it.projectId == p.id }.filter { d ->
            val r = remote[d.date]
            r == null || (d.dirty() && compareVersions(d.stamp(), r.stamp) >= 0)
        }
        tick(planned = todo.size)
        for (day in todo) {
            pushDay(c, p, dirs, day, remote[day.date], gone[day.date])
            tick(finished = 1)
        }

        for (t in allTombs.filter { it.kind == TombstoneKind.DAY && it.projectId == p.id }) {
            val date = LocalDate.parse(t.key)
            val keep = days.getDay(p.id, date)
            val r = remote[date]
            if ((keep != null && !keep.draft && keep.updatedAt >= t.deletedAt) || (r != null && r.stamp.at > t.deletedAt)) {
                tombstones.clear(t.kind, t.projectId, t.key)
                continue
            }
            r?.let { drive.trash(it.file.id) }
            drive.upload(
                dirs.deleted, "$date.json", recordProps("tomb", t.deletedAt, me) + ("sliverDate" to date.toString()),
                "{}".encodeToByteArray(), gone[date]?.id,
            )
            tombstones.clear(t.kind, t.projectId, t.key)
        }
        pushFilms(c, p, dirs, allTombs)
    }

    private suspend fun pushDay(c: Cycle, p: Project, dirs: ProjectDirs, day: Day, r: RemoteDay?, tomb: DriveFile?) = with(c) {
        val by = day.stamp().by
        if (r != null && r.stamp == day.stamp()) {
            markPushed(day, r.file.id)
            return@with
        }
        if (r != null && r.stamp.at == 0L && r.stamp.by.isEmpty() && r.file.id == day.driveFileId && day.backup == BackupState.DONE) {
            drive.setProperties(r.file.id, dayProps(day, by, r.hash ?: ""))
            markPushed(day, r.file.id)
            return@with
        }
        val bytes = reader.read(day.file)
        if (bytes == null) {
            days.save(day.copy(backup = BackupState.FAILED))
            return@with
        }
        val hash = contentHash(bytes)
        if (r != null && r.stamp.by != me && r.file.id != day.driveFileId && r.hash != hash) archiveRemote(dirs, r)
        val file = drive.upload(dirs.root, "${day.date}.mp4", dayProps(day, by, hash), bytes, r?.file?.id)
        if (tomb != null && tomb.stamp.at <= day.updatedAt) drive.trash(tomb.id)
        markPushed(day, file.id)
    }

    private suspend fun markPushed(sent: Day, fileId: String) {
        val current = days.getDay(sent.projectId, sent.date) ?: return
        days.save(
            if (current.updatedAt == sent.updatedAt) current.copy(driveFileId = fileId, backup = BackupState.DONE)
            else current.copy(driveFileId = fileId),
        )
    }

    private suspend fun pushFilms(c: Cycle, p: Project, dirs: ProjectDirs, allTombs: List<Tombstone>) = with(c) {
        val remote = RecordListing(drive.list(dirs.films))
        for (f in films.observeAllProjects().first().filter { it.projectId == p.id }) {
            val r = remote.live[f.id]
            if (r != null && (!f.dirty() || compareVersions(f.stamp(), r.stamp) <= 0)) continue
            drive.upload(dirs.films, "${f.id}.json", recordProps("film", f.updatedAt, f.stamp().by), filmJson(f).encodeToByteArray(), r?.id)
            remote.gone[f.id]?.let { drive.trash(it.id) }
        }
        for (t in allTombs.filter { it.kind == TombstoneKind.FILM && it.projectId == p.id }) {
            val r = remote.live[t.key]
            val keep = films.getFilm(t.key)
            if (keep != null && keep.updatedAt >= t.deletedAt || r != null && r.stamp.at > t.deletedAt) {
                tombstones.clear(t.kind, t.projectId, t.key)
                continue
            }
            r?.let { drive.trash(it.id) }
            drive.upload(dirs.films, "${t.key}$DELETED_SUFFIX", recordProps("tomb", t.deletedAt, me), "{}".encodeToByteArray(), remote.gone[t.key]?.id)
            tombstones.clear(t.kind, t.projectId, t.key)
        }
    }

    // ---- replaced clips and garbage ----

    /** Copies a clip that lost a conflict to replaced/<date>-<deviceId>.mp4, kept for 30 days. */
    private suspend fun archive(dirs: ProjectDirs, date: LocalDate, by: String, bytes: ByteArray, updatedAt: Long) {
        val folder = dirs.replaced ?: drive.ensureSubfolder(dirs.root, REPLACED_DIR).also { dirs.replaced = it }
        val name = "$date-$by.mp4"
        val existing = drive.list(folder).firstOrNull { it.name == name }
        val props = recordProps("replaced", updatedAt, by) + ("sliverDate" to date.toString()) + ("replacedAt" to nowMillis().toString())
        drive.upload(folder, name, props, bytes, existing?.id)
    }

    private suspend fun archiveRemote(dirs: ProjectDirs, r: RemoteDay) {
        archive(dirs, r.date, r.stamp.by.ifEmpty { LEGACY_DEVICE }, drive.download(r.file.id), r.stamp.at)
    }

    private suspend fun collectGarbage(c: Cycle, projectsDir: String, dirs: Collection<ProjectDirs>) {
        val now = nowMillis()
        suspend fun sweep(folder: String, ageOf: (DriveFile) -> Long, keepMs: Long, match: (DriveFile) -> Boolean) {
            for (f in drive.list(folder)) if (match(f) && now - ageOf(f) > keepMs) drive.trash(f.id)
        }
        sweep(projectsDir, { it.stamp.at }, TOMBSTONE_KEEP_MS) { it.name.endsWith(DELETED_SUFFIX) }
        for (d in dirs) {
            sweep(d.deleted, { it.stamp.at }, TOMBSTONE_KEEP_MS) { it.name.endsWith(".json") }
            sweep(d.films, { it.stamp.at }, TOMBSTONE_KEEP_MS) { it.name.endsWith(DELETED_SUFFIX) }
            d.replaced?.let { sweep(it, { f -> f.appProperties["replacedAt"]?.toLongOrNull() ?: 0L }, REPLACED_KEEP_MS) { true } }
        }
    }
}

const val DELETED_SUFFIX = ".deleted.json"
