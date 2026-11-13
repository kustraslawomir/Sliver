package app.sliver

import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.backup.BackupError
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.changedOnAnotherPhone
import app.sliver.data.backup.syncStatusLine
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.DriveFile
import app.sliver.data.backup.InMemoryDriveClient
import app.sliver.data.backup.REPLACED_DIR
import app.sliver.data.backup.PROJECTS_DIR
import app.sliver.data.backup.RestoreEngine
import app.sliver.data.backup.RestoredDayWriter
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.TombstoneKind
import app.sliver.data.backup.TombstoneStore
import app.sliver.data.backup.buildManifest
import app.sliver.data.backup.compareVersions
import app.sliver.data.backup.contentHash
import app.sliver.data.backup.stampDay
import app.sliver.data.backup.stampFilm
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Tombstone
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.data.settings.AccountSettings
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.SettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

private class Clock(var now: Long = 1_000L)

private class MemSettings(deviceId: String) : SettingsRepository {
    private val state = MutableStateFlow(AppSettings(account = AccountSettings(signedIn = true, deviceId = deviceId)))
    override val settings = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
}

private class MemTombstones : TombstoneStore {
    val rows = mutableListOf<Tombstone>()
    override suspend fun record(kind: String, projectId: String, key: String, at: Long) {
        rows.removeAll { it.kind == kind && it.projectId == projectId && it.key == key }
        rows += Tombstone(kind, projectId, key, at)
    }
    override suspend fun all() = rows.toList()
    override suspend fun clear(kind: String, projectId: String, key: String) {
        rows.removeAll { it.kind == kind && it.projectId == projectId && it.key == key }
    }
}

private class MemFiles : FileStore {
    val store = mutableMapOf<String, ByteArray>()
    private var n = 0
    override fun dayPath(projectId: String, date: LocalDate) = "$projectId/$date.mp4"
    override fun thumbPath(projectId: String, date: LocalDate) = "$projectId/$date.jpg"
    override fun filmPath(filmId: String) = "films/$filmId.mp4"
    override fun newTmpPath(timestampMillis: Long) = "tmp/${n++}"
    override fun exists(path: String) = path in store
    override fun delete(path: String) = store.remove(path) != null
    override fun move(from: String, to: String): Boolean {
        store[to] = store.remove(from) ?: return false
        return true
    }
    override fun freeSpaceBytes() = Long.MAX_VALUE
    override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long) = 0
}

private class MemDays(private val projects: FakeProjects, private val clock: Clock, private val tombs: TombstoneStore) : DayRepository {
    val rows = MutableStateFlow(emptyMap<Pair<String, LocalDate>, Day>())
    private val confirmed get() = rows.map { m -> m.values.filter { !it.draft }.sortedBy { d -> d.date } }
    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>> = observeAll()
    override fun observeAll(): Flow<List<Day>> = confirmed
    override fun observeDay(date: LocalDate) = rows.map { null as Day? }
    override fun observeCount() = confirmed.map { it.size }
    override suspend fun getDay(date: LocalDate) = rows.value[projects.activeId() to date]
    override suspend fun getDay(projectId: String, date: LocalDate) = rows.value[projectId to date]
    override fun observeAllProjects() = confirmed
    override fun observeDrafts(): Flow<List<Day>> = rows.map { m -> m.values.filter { it.draft } }
    override suspend fun draftsOfRun(projectId: String, runId: String): List<Day> = emptyList()
    override suspend fun deleteRow(projectId: String, date: LocalDate) { rows.value = rows.value - (projectId to date) }
    override suspend fun save(day: Day) {
        val full = day.copy(projectId = day.projectId.ifEmpty { projects.activeId() })
        rows.value = rows.value + ((full.projectId to full.date) to stampDay(rows.value[full.projectId to full.date], full, clock.now))
    }
    override suspend fun delete(date: LocalDate) {
        val id = projects.activeId()
        if (rows.value[id to date] == null) return
        rows.value = rows.value - (id to date)
        tombs.record(TombstoneKind.DAY, id, date.toString(), clock.now)
    }
    override suspend fun applyRemote(day: Day) { rows.value = rows.value + ((day.projectId to day.date) to day) }
    override suspend fun removeRemote(projectId: String, date: LocalDate) { rows.value = rows.value - (projectId to date) }
    override suspend fun saveClip(request: SaveClipRequest): Day = error("unused")
}

private class MemFilms(private val projects: FakeProjects, private val clock: Clock, private val tombs: TombstoneStore) : FilmRepository {
    val rows = MutableStateFlow(emptyMap<String, Film>())
    override fun observeAll(): Flow<List<Film>> = rows.map { it.values.toList() }
    override fun observeAllProjects(): Flow<List<Film>> = observeAll()
    override fun observeFilm(id: String) = rows.map { it[id] }
    override fun observeCount() = rows.map { it.size }
    override suspend fun getFilm(id: String) = rows.value[id]
    override suspend fun save(film: Film) {
        val full = film.copy(projectId = film.projectId.ifEmpty { projects.activeId() })
        rows.value = rows.value + (full.id to stampFilm(rows.value[full.id], full, clock.now))
    }
    override suspend fun delete(id: String) {
        val f = rows.value[id] ?: return
        rows.value = rows.value - id
        tombs.record(TombstoneKind.FILM, f.projectId, id, clock.now)
    }
    override suspend fun applyRemote(film: Film) { rows.value = rows.value + (film.id to film) }
    override suspend fun removeRemote(id: String) { rows.value = rows.value - id }
}

/** One phone: its own database, files and settings, talking to a Drive shared with the other phones. */
private class Phone(val name: String, drive: InMemoryDriveClient, val clock: Clock, projectList: List<app.sliver.data.db.Project> = emptyList()) {
    val tombs = MemTombstones()
    val files = MemFiles()
    val settings = MemSettings(name)
    val projects = FakeProjects(projectList, tombstones = tombs, now = { clock.now })
    val days = MemDays(projects, clock, tombs)
    val films = MemFilms(projects, clock, tombs)
    val engine = SyncEngine(
        drive, days, films, settings, DayFileReader { files.store[it] }, projects, files,
        RestoredDayWriter { bytes, video, thumb -> files.store[video] = bytes; files.store[thumb] = byteArrayOf(); 0 },
        tombs, nowMillis = { clock.now },
    )

    /** Records a clip locally as the app does: file on disk plus a day row, stamped by the repository. */
    suspend fun record(date: String, content: Int, projectId: String = "p1", draft: Boolean = false) {
        val d = LocalDate.parse(date)
        files.store[files.dayPath(projectId, d)] = byteArrayOf(content.toByte(), 9, 9)
        days.save(
            Day(
                projectId = projectId, date = d, file = files.dayPath(projectId, d), thumb = files.thumbPath(projectId, d),
                source = DaySource.CAMERA, takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(clock.now),
                draft = draft,
            ),
        )
    }

    fun clip(date: String, projectId: String = "p1") = files.store[files.dayPath(projectId, LocalDate.parse(date))]?.first()?.toInt()
    suspend fun day(date: String, projectId: String = "p1") = days.getDay(projectId, LocalDate.parse(date))
    suspend fun sync(): BackupError? = engine.run()
}

class SyncTest {
    private val drive = InMemoryDriveClient()
    private val clock = Clock()
    private val a = Phone("phone-a", drive, clock, listOf(project("p1", "Diary")))
    private val b = Phone("phone-b", drive, clock)

    private fun tick(ms: Long = 1_000) { clock.now += ms }

    private suspend fun names(vararg path: String): List<String> {
        var folder = drive.ensureFolder()
        for (part in path) folder = drive.listFolders(folder).first { it.name == part }.id
        return drive.list(folder).map { it.name }.sorted()
    }

    private suspend fun fileBytes(vararg path: String, name: String): ByteArray {
        var folder = drive.ensureFolder()
        for (part in path) folder = drive.listFolders(folder).first { it.name == part }.id
        return drive.download(drive.list(folder).first { it.name == name }.id)
    }

    private suspend fun replacedNames(projectId: String) = runCatching { names(projectId, REPLACED_DIR) }.getOrDefault(emptyList())

    @Test
    fun clipRecordedOnAAppearsOnB() = runTest {
        a.record("2025-03-01", 1)
        tick()
        assertNull(a.sync())
        tick()
        assertNull(b.sync())

        assertEquals("Diary", b.projects.all.value.single().name)
        assertEquals(1, b.clip("2025-03-01"))
        val day = b.day("2025-03-01")!!
        assertEquals(BackupState.DONE, day.backup)
        assertEquals("phone-a", day.updatedBy)
        assertNull(a.sync())
        assertEquals(listOf("2025-03-01.mp4"), names("p1").filter { it.endsWith(".mp4") })
    }

    @Test
    fun secondSyncWithoutChangesTransfersNothing() = runTest {
        a.record("2025-03-01", 1)
        tick(); a.sync()
        tick(); b.sync()
        drive.uploadsBeforeFailure = 0
        drive.downloadsBeforeFailure = 0
        tick(); assertNull(a.sync())
        tick(); assertNull(b.sync())
    }

    @Test
    fun sameDateOnBothPhonesNewerWinsAndLoserIsKept() = runTest {
        a.record("2025-03-02", 1)
        a.sync()
        tick(); b.projects.restore(project("p1", "Diary")); b.projects.setActive("p1")
        tick(); b.record("2025-03-02", 2)
        tick()
        assertNull(b.sync())
        tick()
        assertNull(a.sync())

        assertEquals(2, a.clip("2025-03-02"))
        assertEquals(2, b.clip("2025-03-02"))
        assertEquals(listOf("2025-03-02-phone-a.mp4"), replacedNames("p1"))
        assertEquals(1, fileBytes("p1", REPLACED_DIR, name = "2025-03-02-phone-a.mp4").first().toInt())
        assertEquals(listOf("p1:2025-03-02"), a.settings.settings.first().account.changedNotices.map { it.replace('|', ':') })
        assertTrue(b.settings.settings.first().account.changedNotices.isEmpty())
    }

    @Test
    fun sameDateOfflineOnBothThenOlderSyncsLast() = runTest {
        a.record("2025-03-03", 1)
        tick(); b.projects.restore(project("p1", "Diary")); b.projects.setActive("p1")
        tick(); b.record("2025-03-03", 2)
        tick(); assertNull(b.sync())
        tick(); assertNull(a.sync())
        tick(); assertNull(b.sync())

        assertEquals(2, a.clip("2025-03-03"))
        assertEquals(2, b.clip("2025-03-03"))
        assertEquals(1, replacedNames("p1").size)
        assertEquals(listOf("2025-03-03.mp4"), names("p1").filter { it.endsWith(".mp4") })
    }

    @Test
    fun tieOnTimeIsBrokenByDeviceIdOnBothPhones() = runTest {
        a.record("2025-03-04", 1)
        b.projects.restore(project("p1", "Diary")); b.projects.setActive("p1")
        b.record("2025-03-04", 2)
        assertNull(a.sync()); assertNull(b.sync()); assertNull(a.sync()); assertNull(b.sync())

        assertEquals(2, a.clip("2025-03-04"))
        assertEquals(2, b.clip("2025-03-04"))
    }

    @Test
    fun deleteOnAIsGoneOnB() = runTest {
        a.record("2025-03-05", 1)
        a.record("2025-03-06", 1)
        tick(); a.sync()
        tick(); b.sync()
        assertEquals(1, b.clip("2025-03-05"))

        tick(); a.days.delete(LocalDate.parse("2025-03-05"))
        tick(); assertNull(a.sync())
        tick(); assertNull(b.sync())

        assertNull(b.day("2025-03-05"))
        assertNull(b.clip("2025-03-05"))
        assertNotNull(b.day("2025-03-06"))
        assertEquals(listOf("2025-03-06.mp4"), names("p1").filter { it.endsWith(".mp4") })
        assertEquals(listOf("2025-03-05.json"), names("p1", "deleted"))
        assertTrue(a.tombs.rows.isEmpty())
        tick(); a.sync()
        assertNull(a.day("2025-03-05"))
    }

    @Test
    fun recordingAgainAfterADeleteBringsTheDayBackEverywhere() = runTest {
        a.record("2025-03-05", 1)
        tick(); a.sync(); tick(); b.sync()
        tick(); a.days.delete(LocalDate.parse("2025-03-05"))
        tick(); a.sync(); tick(); b.sync()
        assertNull(b.day("2025-03-05"))

        tick(); b.record("2025-03-05", 7)
        tick(); b.sync(); tick(); a.sync()

        assertEquals(7, a.clip("2025-03-05"))
        assertEquals(7, b.clip("2025-03-05"))
    }

    @Test
    fun renameOnBIsRenamedOnA() = runTest {
        a.record("2025-03-01", 1)
        tick(); a.sync(); tick(); b.sync()

        tick(); b.projects.rename("p1", "Our year")
        tick(); assertNull(b.sync())
        tick(); assertNull(a.sync())

        assertEquals("Our year", a.projects.all.value.single().name)
        assertEquals(1, a.projects.all.value.size)
    }

    @Test
    fun archivingAndDeletingAProjectTravels() = runTest {
        a.record("2025-03-01", 1)
        tick(); a.sync(); tick(); b.sync()
        tick(); b.projects.setArchived("p1", true)
        tick(); b.sync(); tick(); a.sync()
        assertTrue(a.projects.all.value.single().archived)

        tick(); a.projects.delete("p1")
        tick(); a.sync(); tick(); b.sync()
        assertTrue(b.projects.all.value.isEmpty())
        assertTrue(drive.listFolders(drive.ensureFolder()).none { it.name == "p1" })
    }

    @Test
    fun filmCreatedOnAHasItsDefinitionOnBAndRendersLocally() = runTest {
        a.record("2025-03-01", 1)
        a.films.save(Film(id = "f1", name = "Spring", projectId = "p1", look = app.sliver.data.LookPreset.TAPE, lookIntensity = 60, lookGrain = 20))
        tick(); a.sync(); tick(); b.sync()

        val onB = b.films.getFilm("f1")!!
        assertEquals("Spring", onB.name)
        assertEquals(60, onB.lookIntensity)
        assertEquals(20, onB.lookGrain)
        assertEquals(app.sliver.data.LookPreset.TAPE, onB.look)
        assertNull(onB.renderPath)

        // B renders locally; saving the render is not an edit, so nothing goes back and A's definition stays.
        tick(); b.films.save(onB.copy(renderPath = "films/f1.mp4", renderedAt = Instant.fromEpochMilliseconds(clock.now)))
        assertEquals(onB.updatedAt, b.films.getFilm("f1")!!.updatedAt)
        tick(); b.sync(); tick(); a.sync()
        assertNull(a.films.getFilm("f1")!!.renderPath)
        assertEquals("films/f1.mp4", b.films.getFilm("f1")!!.renderPath)
        assertTrue(names("p1", "films").single() == "f1.json")
    }

    @Test
    fun editedFilmDropsTheOtherPhonesStaleRender() = runTest {
        a.films.save(Film(id = "f1", name = "Spring", projectId = "p1"))
        a.record("2025-03-01", 1)
        tick(); a.sync(); tick(); b.sync()
        b.films.save(b.films.getFilm("f1")!!.copy(renderPath = "films/f1.mp4"))
        b.files.store["films/f1.mp4"] = byteArrayOf(1)

        tick(); a.films.save(a.films.getFilm("f1")!!.copy(name = "Spring 2"))
        tick(); a.sync(); tick(); b.sync()

        val onB = b.films.getFilm("f1")!!
        assertEquals("Spring 2", onB.name)
        assertNull(onB.renderPath)
        assertFalse(b.files.exists("films/f1.mp4"))
    }

    @Test
    fun deletedFilmIsGoneOnTheOtherPhone() = runTest {
        a.films.save(Film(id = "f1", name = "Spring", projectId = "p1"))
        a.record("2025-03-01", 1)
        tick(); a.sync(); tick(); b.sync()
        assertNotNull(b.films.getFilm("f1"))

        tick(); a.films.delete("f1")
        tick(); a.sync(); tick(); b.sync()

        assertNull(b.films.getFilm("f1"))
        assertEquals(listOf("f1.deleted.json"), names("p1", "films"))
    }

    @Test
    fun draftsAreNeverSynced() = runTest {
        a.record("2025-03-01", 1)
        a.record("2025-03-02", 1, draft = true)
        tick(); a.sync(); tick(); b.sync()

        assertNotNull(b.day("2025-03-01"))
        assertNull(b.day("2025-03-02"))
        assertEquals(listOf("2025-03-01.mp4"), names("p1").filter { it.endsWith(".mp4") })
    }

    @Test
    fun interruptedPushResumesWithoutDuplicates() = runTest {
        for (d in 1..4) a.record("2025-03-0$d", d)
        drive.uploadsBeforeFailure = 3
        tick(); assertEquals(BackupError.OFFLINE, a.sync())
        assertEquals(BackupError.OFFLINE, a.settings.settings.first().account.backupError)

        drive.uploadsBeforeFailure = null
        tick(); assertNull(a.sync())

        assertEquals((1..4).map { "2025-03-0$it.mp4" }, names("p1").filter { it.endsWith(".mp4") })
        assertTrue(a.days.observeAll().first().all { it.backup == BackupState.DONE })
        tick(); b.sync()
        assertEquals(4, b.days.observeAll().first().size)
    }

    @Test
    fun interruptedPullResumesWithoutDuplicates() = runTest {
        for (d in 1..4) a.record("2025-03-0$d", d)
        tick(); a.sync()

        drive.downloadsBeforeFailure = 3
        tick(); assertEquals(BackupError.OFFLINE, b.sync())
        assertTrue(b.days.observeAll().first().size in 1..3)

        drive.downloadsBeforeFailure = null
        tick(); assertNull(b.sync())

        assertEquals(4, b.days.observeAll().first().size)
        assertEquals((1..4).map { it }, (1..4).map { b.clip("2025-03-0$it") })
        assertEquals(4, names("p1").count { it.endsWith(".mp4") })
    }

    @Test
    fun countsPhonesSeenInTheLast30Days() = runTest {
        a.record("2025-03-01", 1)
        tick(); a.sync()
        assertEquals(1, a.settings.settings.first().account.phones)
        tick(); b.sync()
        tick(); b.record("2025-03-02", 2)
        tick(); b.sync()
        tick(); a.sync()
        assertEquals(2, a.settings.settings.first().account.phones)
        assertNotNull(a.settings.settings.first().account.lastSyncAt)
    }

    @Test
    fun oldTombstonesAndReplacedClipsAreCollected() = runTest {
        a.record("2025-03-01", 1)
        a.record("2025-03-02", 1)
        tick(); a.sync()
        tick(); a.days.delete(LocalDate.parse("2025-03-02"))
        tick(); a.sync()
        assertEquals(listOf("2025-03-02.json"), names("p1", "deleted"))

        clock.now += 91L * 24 * 60 * 60 * 1000
        a.sync()
        assertTrue(names("p1", "deleted").isEmpty())
    }

    @Test
    fun deviceIdIsCreatedOnceAndKept() = runTest {
        val fresh = Phone("", drive, clock, listOf(project("p9", "Other")))
        fresh.sync()
        val id = fresh.settings.settings.first().account.deviceId
        assertTrue(id.isNotEmpty())
        fresh.sync()
        assertEquals(id, fresh.settings.settings.first().account.deviceId)
    }

    @Test
    fun driveFullKeepsClipsPendingAndRecovers() = runTest {
        a.record("2025-03-01", 1)
        drive.failUploads = BackupError.DRIVE_FULL
        tick(); assertEquals(BackupError.DRIVE_FULL, a.sync())
        assertEquals(BackupState.PENDING, a.day("2025-03-01")!!.backup)
        assertNotNull(a.settings.settings.first().account.failingSince)

        drive.failUploads = null
        tick(); assertNull(a.sync())
        assertNull(a.settings.settings.first().account.failingSince)
        assertEquals(BackupState.DONE, a.day("2025-03-01")!!.backup)
    }

    // ---- older backups ----

    private val legacyLook = """{"preset":"NATURAL"}"""

    @Test
    fun oldManifestBackupMigratesAndRestoresOnANewPhone() = runTest {
        val root = drive.ensureFolder()
        val folder = drive.ensureSubfolder(root, "Trip")
        drive.upload(folder, "2025-06-01.mp4", mapOf("sliverDate" to "2025-06-01", "look" to legacyLook), byteArrayOf(5, 5))
        drive.writeManifest(
            folder,
            buildManifest(AppSettings(), listOf(Film(id = "f9", name = "Old film", rangeType = FilmRangeType.CUSTOM)), 5L, project("trip", "Trip")),
        )

        assertNull(b.sync())

        assertEquals(listOf("trip"), b.projects.all.value.map { it.id })
        assertEquals("Trip", b.projects.all.value.single().name)
        assertEquals(5, b.clip("2025-06-01", "trip"))
        assertEquals("trip", b.films.getFilm("f9")!!.projectId)
        assertEquals("Old film", b.films.getFilm("f9")!!.name)
        val rootFolders = drive.listFolders(root).map { it.name }.sorted()
        assertEquals(listOf(PROJECTS_DIR, "trip"), rootFolders)
        assertTrue(names("trip").none { it == "manifest.json" })
        assertEquals(BackupState.DONE, b.day("2025-06-01", "trip")!!.backup)

        // The migrated backup then syncs like any other, with nothing re-uploaded for the old clip.
        drive.uploadsBeforeFailure = 0
        tick(); assertNull(b.sync())
        drive.uploadsBeforeFailure = null
        tick(); assertNull(a.sync())
        assertEquals(5, a.clip("2025-06-01", "trip"))
    }

    @Test
    fun oldFlatBackupRestoresIntoTheActiveProject() = runTest {
        val root = drive.ensureFolder()
        drive.upload(root, "2025-02-01.mp4", mapOf("sliverDate" to "2025-02-01", "look" to legacyLook), byteArrayOf(8))
        drive.writeManifest(root, buildManifest(AppSettings(), listOf(Film(id = "f", name = "Old", rangeType = FilmRangeType.CUSTOM)), 1L))

        assertNull(a.sync())

        assertEquals(8, a.clip("2025-02-01", "p1"))
        assertEquals("p1", a.films.getFilm("f")!!.projectId)
    }

    @Test
    fun oldBackupNeverOverwritesADayCapturedHere() = runTest {
        val root = drive.ensureFolder()
        val folder = drive.ensureSubfolder(root, "Diary")
        drive.upload(folder, "2025-01-01.mp4", mapOf("sliverDate" to "2025-01-01", "look" to legacyLook), byteArrayOf(3))
        drive.writeManifest(folder, buildManifest(AppSettings(), emptyList(), 1L, project("p1", "Diary")))
        a.record("2025-01-01", 4)
        tick()

        assertNull(a.sync())

        assertEquals(4, a.clip("2025-01-01"))
        tick(); b.sync()
        assertEquals(4, b.clip("2025-01-01"))
        assertEquals(listOf("2025-01-01-legacy.mp4"), replacedNames("p1"))
    }

    @Test
    fun restoreEngineScansAndRunsTheFirstPull() = runTest {
        a.record("2025-03-01", 1)
        a.record("2025-03-02", 1)
        tick(); a.sync()

        val restore = RestoreEngine(b.engine, b.days, b.projects)
        val scan = restore.scan()
        assertEquals(listOf(LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-02")), scan.items.map { it.date })
        assertEquals(setOf("p1"), scan.items.map { it.projectId }.toSet())
        assertNull(restore.run())
        assertEquals(2, b.days.observeAll().first().size)
        assertEquals("p1", b.projects.activeId())
    }
}

class SyncRulesTest {
    private fun day(updatedAt: Long = 0, by: String = "") = Day(
        projectId = "p", date = LocalDate(2025, 1, 1), file = "f", thumb = "t", source = DaySource.CAMERA,
        takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0),
        backup = BackupState.DONE, driveFileId = "x", updatedAt = updatedAt, updatedBy = by,
    )

    @Test
    fun bookkeepingSavesKeepTheStamp() {
        val existing = day(updatedAt = 100, by = "other")
        val saved = stampDay(existing, existing.copy(driveFileId = "y", luma = 0.4f, tone = 7, color = 3, thumb = "t2"), now = 999)
        assertEquals(100, saved.updatedAt)
        assertEquals("other", saved.updatedBy)
        assertEquals(BackupState.DONE, saved.backup)
    }

    @Test
    fun contentChangesAreNewEditsByThisPhoneAndNeedBackup() {
        val existing = day(updatedAt = 100, by = "other")
        val saved = stampDay(existing, existing.copy(motion = app.sliver.data.Motion.ZOOM), now = 999)
        assertEquals(999, saved.updatedAt)
        assertEquals("", saved.updatedBy)
        assertEquals(BackupState.PENDING, saved.backup)
        assertEquals(999, stampDay(null, existing, now = 999).updatedAt)
    }

    @Test
    fun renderingAFilmIsNotAnEdit() {
        val film = Film(id = "f", name = "N", updatedAt = 50, updatedBy = "x")
        assertEquals(50, stampFilm(film, film.copy(renderPath = "r", renderedAt = Instant.fromEpochMilliseconds(5)), 99).updatedAt)
        assertEquals(99, stampFilm(film, film.copy(name = "M"), 99).updatedAt)
        assertEquals(99, stampFilm(film, film.copy(lookIntensity = 20), 99).updatedAt)
    }

    @Test
    fun lastWriterWinsAndTiesGoToTheLargerDeviceId() {
        assertTrue(compareVersions(10, "a", 5, "z") > 0)
        assertTrue(compareVersions(5, "b", 5, "a") > 0)
        assertEquals(0, compareVersions(5, "a", 5, "a"))
    }

    @Test
    fun hashTellsCopiesApart() {
        assertEquals(contentHash(byteArrayOf(1, 2, 3)), contentHash(byteArrayOf(1, 2, 3)))
        assertFalse(contentHash(byteArrayOf(1, 2, 3)) == contentHash(byteArrayOf(1, 2, 4)))
        assertFalse(contentHash(byteArrayOf()) == contentHash(byteArrayOf(0)))
    }
}

class SyncUiTest {
    @Test
    fun statusLineSaysHowLongAgoAndHowManyPhones() {
        val now = 10_000_000L
        assertEquals("Not synced yet", syncStatusLine(null, now, 1))
        assertEquals("Synced · just now · 1 phone", syncStatusLine(now - 20_000, now, 1))
        assertEquals("Synced · 2 min ago · 2 phones", syncStatusLine(now - 125_000, now, 2))
        assertEquals("Synced · 3 h ago · 2 phones", syncStatusLine(now - 3 * 3_600_000L, now, 2))
        assertEquals("Synced · 2 d ago · 1 phone", syncStatusLine(now - 2 * 86_400_000L - 5, now, 1))
    }

    @Test
    fun changedNoticeNamesTheDayOrCountsThem() {
        assertNull(changedOnAnotherPhone(emptyList()))
        assertEquals("10 Nov was changed on another phone", changedOnAnotherPhone(listOf(LocalDate(2026, 11, 10))))
        assertEquals("2 clips were changed on another phone", changedOnAnotherPhone(listOf(LocalDate(2026, 11, 10), LocalDate(2026, 11, 11))))
    }
}

private class RecordingScheduler : BackupScheduler {
    var runs = 0
    var periodic = 0
    var cancelled = 0
    override fun schedule(wifiOnly: Boolean, anyNetwork: Boolean) { runs++ }
    override fun keepSynced(wifiOnly: Boolean) { periodic++ }
    override fun cancel() { cancelled++ }
}

class SyncTriggerTest {
    @Test
    fun syncsOnStartThenAgainThirtySecondsAfterASaveOrDelete() = runTest {
        val phone = Phone("a", InMemoryDriveClient(), Clock(), listOf(project("p1", "Diary")))
        val scheduler = RecordingScheduler()
        BackupCoordinator(phone.days, phone.settings, scheduler).start(backgroundScope)
        runCurrent()
        assertEquals(1, scheduler.runs)
        assertEquals(1, scheduler.periodic)

        phone.record("2025-01-01", 1)
        advanceTimeBy(29_000); runCurrent()
        assertEquals(1, scheduler.runs)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(2, scheduler.runs)

        phone.days.delete(LocalDate.parse("2025-01-01"))
        advanceTimeBy(31_000); runCurrent()
        assertEquals(3, scheduler.runs)
    }

    @Test
    fun signingOutStopsEverything() = runTest {
        val phone = Phone("a", InMemoryDriveClient(), Clock(), listOf(project("p1", "Diary")))
        val scheduler = RecordingScheduler()
        BackupCoordinator(phone.days, phone.settings, scheduler).start(backgroundScope)
        runCurrent()
        phone.settings.update { it.copy(account = it.account.copy(signedIn = false)) }
        runCurrent()
        assertEquals(1, scheduler.cancelled)
        phone.record("2025-01-01", 1)
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, scheduler.runs)
    }

    @Test
    fun syncedDaysFromAnotherPhoneDoNotTriggerAnotherSync() = runTest {
        val phone = Phone("a", InMemoryDriveClient(), Clock(), listOf(project("p1", "Diary")))
        val scheduler = RecordingScheduler()
        BackupCoordinator(phone.days, phone.settings, scheduler).start(backgroundScope)
        runCurrent()
        phone.days.applyRemote(
            Day(projectId = "p1", date = LocalDate(2025, 1, 1), file = "f", thumb = "t", source = DaySource.CAMERA,
                takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0), updatedAt = 5, updatedBy = "b"),
        )
        advanceTimeBy(120_000); runCurrent()
        assertEquals(1, scheduler.runs)
    }
}
