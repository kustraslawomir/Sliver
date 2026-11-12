package app.sliver

import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.backup.BackupEngine
import app.sliver.data.backup.BackupError
import app.sliver.data.backup.BackupSnapshot
import app.sliver.data.backup.BackupUi
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.InMemoryDriveClient
import app.sliver.data.backup.NetworkKind
import app.sliver.data.backup.PROBLEM_AFTER_MILLIS
import app.sliver.data.backup.backupUi
import app.sliver.data.backup.buildManifest
import app.sliver.data.backup.folderNames
import app.sliver.data.backup.formatBytes
import app.sliver.data.backup.shouldNotifyBackupProblem
import app.sliver.data.db.Day
import app.sliver.data.db.Film
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
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

private class Settings : SettingsRepository {
    private val state = MutableStateFlow(AppSettings(account = AccountSettings(signedIn = true)))
    override val settings = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}

private class Days(vararg initial: Day) : DayRepository {
    val rows = MutableStateFlow(initial.associateBy { it.date })
    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>> = observeAll()
    override fun observeAll(): Flow<List<Day>> = rows.map { it.values.sortedBy { d -> d.date } }
    override fun observeDay(date: LocalDate) = rows.map { it[date] }
    override fun observeCount() = rows.map { it.size }
    override suspend fun getDay(date: LocalDate) = rows.value[date]
    override suspend fun getDay(projectId: String, date: LocalDate) = rows.value[date]
    override fun observeAllProjects(): Flow<List<Day>> = observeAll()
    override fun observeDrafts(): Flow<List<Day>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun draftsOfRun(projectId: String, runId: String): List<Day> = emptyList()
    override suspend fun deleteRow(projectId: String, date: LocalDate) = Unit
    override suspend fun save(day: Day) { rows.value = rows.value + (day.date to day) }
    override suspend fun delete(date: LocalDate) { rows.value = rows.value - date }
    override suspend fun saveClip(request: SaveClipRequest): Day = error("unused")
}

private class NoFilms : FilmRepository {
    override fun observeAll(): Flow<List<Film>> = MutableStateFlow(emptyList())
    override fun observeAllProjects(): Flow<List<Film>> = MutableStateFlow(emptyList())
    override fun observeFilm(id: String) = MutableStateFlow<Film?>(null)
    override fun observeCount() = MutableStateFlow(0)
    override suspend fun getFilm(id: String): Film? = null
    override suspend fun save(film: Film) = Unit
    override suspend fun delete(id: String) = Unit
}

private fun day(date: String, backup: BackupState = BackupState.PENDING, driveFileId: String? = null, projectId: String = "p1") = Day(
    date = LocalDate.parse(date), file = "days/$date.mp4", thumb = "t", source = DaySource.CAMERA,
    takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0),
    driveFileId = driveFileId, backup = backup, projectId = projectId,
)

private fun snap(
    pending: Int = 1, running: Boolean = false, error: BackupError? = null,
    wifiOnly: Boolean = true, network: NetworkKind = NetworkKind.UNMETERED,
) = BackupSnapshot(pending, running, error, wifiOnly, network)

class BackupStateTest {
    @Test
    fun mapsAllSevenStates() {
        assertEquals(BackupUi.BACKING_UP, backupUi(snap(running = true)))
        assertEquals(BackupUi.UP_TO_DATE, backupUi(snap(pending = 0)))
        assertEquals(BackupUi.PAUSED_WIFI, backupUi(snap(network = NetworkKind.METERED)))
        assertEquals(BackupUi.FAILED, backupUi(snap(error = BackupError.OTHER)))
        assertEquals(BackupUi.DRIVE_FULL, backupUi(snap(error = BackupError.DRIVE_FULL)))
        assertEquals(BackupUi.REVOKED, backupUi(snap(error = BackupError.REVOKED)))
        assertEquals(BackupUi.OFFLINE, backupUi(snap(network = NetworkKind.NONE)))
    }

    @Test
    fun meteredIsFineWhenWifiOnlyIsOff() {
        assertEquals(BackupUi.BACKING_UP, backupUi(snap(wifiOnly = false, network = NetworkKind.METERED)))
    }

    @Test
    fun problemNotificationOnlyAfter24HoursOnce() {
        val a = AccountSettings(backupError = BackupError.DRIVE_FULL, failingSince = 1000L)
        assertFalse(shouldNotifyBackupProblem(a, true, 1000L + PROBLEM_AFTER_MILLIS - 1))
        assertTrue(shouldNotifyBackupProblem(a, true, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(a, false, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(a.copy(problemNotified = true), true, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(AccountSettings(), true, Long.MAX_VALUE))
    }

    @Test
    fun formatsBytes() {
        assertEquals("512 KB", formatBytes(512_000))
        assertEquals("12.3 MB", formatBytes(12_340_000))
        assertEquals("15.0 GB", formatBytes(15_000_000_000))
    }
}

class BackupEngineTest {
    private val reader = DayFileReader { byteArrayOf(1, 2, 3) }

    @Test
    fun uploadsPendingDaysAndWritesManifest() = runTest {
        val drive = InMemoryDriveClient()
        val days = Days(day("2025-01-01"), day("2025-01-02"), day("2025-01-03", BackupState.DONE, "x"))
        val settings = Settings()
        val engine = BackupEngine(drive, days, NoFilms(), settings, reader, FakeProjects())

        assertNull(engine.run())

        val all = days.observeAll().first()
        assertTrue(all.all { it.backup == BackupState.DONE })
        assertNotNull(all[0].driveFileId)
        val root = settings.settings.first().account.driveFolderId!!
        val folder = drive.listFolders(root).single()
        assertEquals("My 2025", folder.name)
        assertEquals(listOf("2025-01-01.mp4", "2025-01-02.mp4", "manifest.json"), drive.list(folder.id).map { it.name }.sorted())
        assertTrue(drive.readManifest(folder.id)!!.contains("\"projectId\":\"p1\""))
        assertNull(engine.progress.value)
    }

    @Test
    fun replacedDayOverwritesExistingDriveFile() = runTest {
        val drive = InMemoryDriveClient()
        val days = Days(day("2025-01-01"))
        val engine = BackupEngine(drive, days, NoFilms(), Settings(), reader, FakeProjects())
        engine.run()
        val id = days.getDay(LocalDate.parse("2025-01-01"))!!.driveFileId
        days.save(days.getDay(LocalDate.parse("2025-01-01"))!!.copy(backup = BackupState.PENDING))

        engine.run()

        assertEquals(id, days.getDay(LocalDate.parse("2025-01-01"))!!.driveFileId)
    }

    @Test
    fun trashesDriveFilesOfRemovedDays() = runTest {
        val drive = InMemoryDriveClient()
        val days = Days(day("2025-01-01"))
        val settings = Settings()
        val engine = BackupEngine(drive, days, NoFilms(), settings, reader, FakeProjects())
        engine.run()
        val folder = drive.listFolders(settings.settings.first().account.driveFolderId!!).single().id
        val id = days.getDay(LocalDate.parse("2025-01-01"))!!.driveFileId!!
        days.delete(LocalDate.parse("2025-01-01"))
        settings.update { it.copy(account = it.account.copy(pendingTrash = listOf(id))) }

        engine.run()

        assertEquals(listOf("manifest.json"), drive.list(folder).map { it.name })
        assertTrue(settings.settings.first().account.pendingTrash.isEmpty())
    }

    @Test
    fun driveFullRecordsErrorAndKeepsDayPending() = runTest {
        val drive = InMemoryDriveClient().apply { failUploads = BackupError.DRIVE_FULL }
        val days = Days(day("2025-01-01"))
        val settings = Settings()
        val engine = BackupEngine(drive, days, NoFilms(), settings, reader, FakeProjects(), nowMillis = { 42L })

        assertEquals(BackupError.DRIVE_FULL, engine.run())

        val account = settings.settings.first().account
        assertEquals(BackupError.DRIVE_FULL, account.backupError)
        assertEquals(42L, account.failingSince)
        assertEquals(BackupState.PENDING, days.getDay(LocalDate.parse("2025-01-01"))!!.backup)

        drive.failUploads = null
        assertNull(engine.run())
        assertNull(settings.settings.first().account.backupError)
        assertNull(settings.settings.first().account.failingSince)
    }

    @Test
    fun manifestHasSettingsAndFilms() {
        val json = buildManifest(AppSettings(), emptyList(), 5L)
        assertTrue(json.contains("\"dateStamp\":true"))
        assertTrue(json.contains("\"films\":[]"))
    }

    @Test
    fun backsUpEveryActiveProjectInItsOwnFolder() = runTest {
        val drive = InMemoryDriveClient()
        val projects = FakeProjects(listOf(project("p1", "My 2025"), project("p2", "Trip"), project("p3", "Old").copy(archived = true)))
        val days = Days(day("2025-01-01"), day("2025-01-02", projectId = "p2"), day("2025-01-03", projectId = "p3"))
        val settings = Settings()
        assertNull(BackupEngine(drive, days, NoFilms(), settings, reader, projects).run())

        val root = settings.settings.first().account.driveFolderId!!
        val folders = drive.listFolders(root).associateBy { it.name }
        assertEquals(setOf("My 2025", "Trip"), folders.keys)
        assertEquals(listOf("2025-01-02.mp4", "manifest.json"), drive.list(folders.getValue("Trip").id).map { it.name }.sorted())
        assertTrue(drive.readManifest(folders.getValue("Trip").id)!!.contains("\"projectId\":\"p2\""))
        assertEquals(BackupState.PENDING, days.getDay(LocalDate.parse("2025-01-03"))!!.backup)
    }

    @Test
    fun sameNamedProjectsGetDisambiguatedFolders() = runTest {
        val drive = InMemoryDriveClient()
        val projects = FakeProjects(listOf(project("abcd1", "Diary"), project("wxyz2", "Diary")))
        val days = Days(day("2025-01-01", projectId = "abcd1"), day("2025-01-02", projectId = "wxyz2"))
        val settings = Settings()
        assertNull(BackupEngine(drive, days, NoFilms(), settings, reader, projects).run())

        val root = settings.settings.first().account.driveFolderId!!
        assertEquals(setOf("Diary (abcd)", "Diary (wxyz)"), drive.listFolders(root).map { it.name }.toSet())
    }

    @Test
    fun renamedProjectRenamesItsFolder() = runTest {
        val drive = InMemoryDriveClient()
        val projects = FakeProjects()
        val settings = Settings()
        val engine = BackupEngine(drive, Days(day("2025-01-01")), NoFilms(), settings, reader, projects)
        engine.run()
        val root = settings.settings.first().account.driveFolderId!!
        val before = drive.listFolders(root).single().id

        projects.all.value = listOf(project("p1", "Renamed"))
        engine.run()

        val after = drive.listFolders(root).single()
        assertEquals(before, after.id)
        assertEquals("Renamed", after.name)
    }

    @Test
    fun adoptsLegacyFolderWithoutProjectId() = runTest {
        val drive = InMemoryDriveClient()
        val settings = Settings()
        val root = drive.ensureFolder(null)
        settings.update { it.copy(account = it.account.copy(driveFolderId = root)) }
        val legacy = drive.ensureSubfolder(root, "My 2025")
        drive.writeManifest(legacy, "{\"version\":1}")

        assertNull(BackupEngine(drive, Days(day("2025-01-01")), NoFilms(), settings, reader, FakeProjects()).run())

        assertEquals(listOf(legacy), drive.listFolders(root).map { it.id })
    }

    @Test
    fun folderNamesOnlyDisambiguateCollisions() {
        val names = folderNames(listOf(project("aaaa9", "A"), project("bbbb9", "a"), project("cccc9", "B")))
        assertEquals(mapOf("aaaa9" to "A (aaaa)", "bbbb9" to "a (bbbb)", "cccc9" to "B"), names)
    }
}
