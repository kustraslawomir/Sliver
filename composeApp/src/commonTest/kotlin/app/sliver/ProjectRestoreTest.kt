package app.sliver

import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.backup.InMemoryDriveClient
import app.sliver.data.backup.RestoreEngine
import app.sliver.data.backup.RestoredDayWriter
import app.sliver.data.backup.buildManifest
import app.sliver.data.backup.parseManifestProject
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.SaveClipRequest
import app.sliver.data.settings.AccountSettings
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.SettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

private class MemSettings : SettingsRepository {
    private val state = MutableStateFlow(AppSettings(account = AccountSettings(signedIn = true)))
    override val settings = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
}

private class MultiDays : DayRepository {
    val rows = MutableStateFlow(emptyMap<Pair<String, LocalDate>, Day>())
    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Day>> = observeAll()
    override fun observeAll(): Flow<List<Day>> = rows.map { it.values.toList() }
    override fun observeDay(date: LocalDate) = rows.map { null as Day? }
    override fun observeCount() = rows.map { it.size }
    override suspend fun getDay(date: LocalDate): Day? = null
    override suspend fun getDay(projectId: String, date: LocalDate) = rows.value[projectId to date]
    override fun observeAllProjects() = observeAll()
    override suspend fun save(day: Day) { rows.value = rows.value + ((day.projectId to day.date) to day) }
    override suspend fun delete(date: LocalDate) = Unit
    override suspend fun saveClip(request: SaveClipRequest): Day = error("unused")
}

private class SavedFilms : FilmRepository {
    val saved = mutableListOf<Film>()
    override fun observeAll(): Flow<List<Film>> = MutableStateFlow(saved)
    override fun observeAllProjects(): Flow<List<Film>> = MutableStateFlow(saved)
    override fun observeFilm(id: String) = MutableStateFlow<Film?>(null)
    override fun observeCount() = MutableStateFlow(0)
    override suspend fun getFilm(id: String): Film? = saved.firstOrNull { it.id == id }
    override suspend fun save(film: Film) { saved += film }
    override suspend fun delete(id: String) = Unit
}

private class MemFiles : FileStore {
    override fun dayPath(projectId: String, date: LocalDate) = "$projectId/$date.mp4"
    override fun thumbPath(projectId: String, date: LocalDate) = "$projectId/$date.jpg"
    override fun filmPath(filmId: String) = filmId
    private var n = 0
    override fun newTmpPath(timestampMillis: Long) = "tmp/${n++}"
    override fun exists(path: String) = true
    override fun delete(path: String) = true
    override fun move(from: String, to: String) = true
    override fun freeSpaceBytes() = Long.MAX_VALUE
    override fun cleanTmp(nowMillis: Long, maxAgeMillis: Long) = 0
}

class ProjectRestoreTest {
    private val look = Json.encodeToString(app.sliver.data.LookParams.serializer(), app.sliver.data.LookParams())

    private suspend fun InMemoryDriveClient.clip(folder: String, date: String) =
        upload(folder, "$date.mp4", mapOf("sliverDate" to date, "look" to look), byteArrayOf(1))

    private fun engine(drive: InMemoryDriveClient, days: MultiDays, projects: FakeProjects, films: SavedFilms = SavedFilms()) =
        RestoreEngine(drive, days, films, MemSettings(), MemFiles(), RestoredDayWriter { _, _, _ -> 0 }, projects)

    @Test
    fun manifestCarriesProjectAndStaysReadableWithout() {
        val json = buildManifest(AppSettings(), emptyList(), 1L, project("p9", "Trip"))
        val p = parseManifestProject(json)!!
        assertEquals("p9", p.id)
        assertEquals("Trip", p.name)
        assertEquals(FilmFormat.CINEMA, p.format)
        assertNull(parseManifestProject(buildManifest(AppSettings(), emptyList(), 1L)))
        assertNull(parseManifestProject("""{"version":1,"films":[]}"""))
    }

    @Test
    fun restoresProjectFolderIntoItsOwnProjectNotTheActiveOne() = runTest {
        val drive = InMemoryDriveClient()
        val root = drive.ensureFolder()
        val folder = drive.ensureSubfolder(root, "Trip")
        drive.clip(folder, "2025-06-01")
        drive.writeManifest(folder, buildManifest(AppSettings(), emptyList(), 1L, project("trip", "Trip")))
        val days = MultiDays()
        val projects = FakeProjects()

        assertNull(engine(drive, days, projects).run())

        assertEquals(listOf("p1", "trip"), projects.all.value.map { it.id })
        assertEquals(listOf("trip" to LocalDate(2025, 6, 1)), days.rows.value.keys.toList())
        assertEquals("trip", days.rows.value.values.single().projectId)
    }

    @Test
    fun legacyFlatBackupRestoresIntoActiveProject() = runTest {
        val drive = InMemoryDriveClient()
        val root = drive.ensureFolder()
        drive.clip(root, "2025-02-01")
        drive.writeManifest(root, buildManifest(AppSettings(), listOf(Film(id = "f", name = "Old", rangeType = app.sliver.data.FilmRangeType.CUSTOM)), 1L))
        val days = MultiDays()
        val films = SavedFilms()

        assertNull(engine(drive, days, FakeProjects(), films).run())

        assertEquals(listOf("p1" to LocalDate(2025, 2, 1)), days.rows.value.keys.toList())
        assertEquals("p1", films.saved.single().projectId)
    }

    @Test
    fun existingDayInTheTargetProjectIsNotOverwritten() = runTest {
        val drive = InMemoryDriveClient()
        val root = drive.ensureFolder()
        val folder = drive.ensureSubfolder(root, "My 2025")
        drive.clip(folder, "2025-01-01")
        drive.writeManifest(folder, buildManifest(AppSettings(), emptyList(), 1L, project("p1", "My 2025")))
        val days = MultiDays()
        val mine = Day(
            projectId = "p1", date = LocalDate(2025, 1, 1), file = "mine", thumb = "t", source = DaySource.CAMERA,
            takenAt = Instant.fromEpochMilliseconds(0), createdAt = Instant.fromEpochMilliseconds(0), backup = BackupState.PENDING,
        )
        days.save(mine)

        engine(drive, days, FakeProjects()).run()

        assertEquals("mine", days.rows.value.values.single().file)
    }
}
