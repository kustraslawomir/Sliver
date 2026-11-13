package app.sliver.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.ExportedClip
import app.sliver.data.export.ShortMode
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.repo.ProjectRepositoryImpl
import app.sliver.data.repo.SaveClipRequest
import app.sliver.data.settings.DataStoreSettingsRepository
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private object FakeExporter : ClipExporter {
    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropX: Float?, cropY: Float?, format: FilmFormat,
        videoPath: String, thumbPath: String, shortMode: ShortMode,
        look: LookParams,
    ): ExportedClip {
        File(videoPath).writeText("v")
        File(thumbPath).writeText("t")
        return ExportedClip(videoPath, thumbPath, 1000)
    }

    override suspend fun photoToClip(
        source: String, motion: app.sliver.data.Motion, format: FilmFormat, videoPath: String, thumbPath: String,
        look: LookParams, cropX: Float?, cropY: Float?
    ) = exportClip(source, 0, 1f, null, null, format, videoPath, thumbPath, ShortMode.SLOW_TO_FIT)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class DataLayerTest {
    private lateinit var db: SliverDatabase
    private lateinit var days: DayRepositoryImpl
    private lateinit var films: FilmRepositoryImpl
    private lateinit var projects: ProjectRepositoryImpl
    private lateinit var tombs: app.sliver.data.backup.DaoTombstoneStore
    private var clock = 5_000L

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        val files = AndroidFileStore(ctx.filesDir, ctx.cacheDir)
        val settings = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
                File.createTempFile("prefs", ".preferences_pb").apply { delete() }
            },
        )
        tombs = app.sliver.data.backup.DaoTombstoneStore(db.tombstoneDao())
        projects = ProjectRepositoryImpl(db.projectDao(), settings, db.dayDao(), db.filmDao(), files, app.sliver.data.backup.DriveTrashQueue {}, tombs, nowMillis = { clock })
        days = DayRepositoryImpl(db.dayDao(), projects, FakeExporter, files, tombstones = tombs, nowMillis = { clock })
        films = FilmRepositoryImpl(db.filmDao(), projects, tombs, nowMillis = { clock })
    }

    @Test
    fun saveClipWritesFilesAndRow() = runTest {
        val date = LocalDate(2026, 3, 1)
        val day = days.saveClip(
            SaveClipRequest(date, "/src.mp4", 0, 1f, null, FilmFormat.CINEMA, DaySource.GALLERY, Instant.fromEpochSeconds(0)),
        )
        assertEquals(day.file, days.getDay(date)?.file)
        assertEquals("v", File(day.file).readText())
        assertEquals("t", File(day.thumb).readText())
    }

    @Test
    fun deleteProjectRemovesClipsFilmsAndFiles() = runTest {
        val first = projects.activeId()
        val saved = days.saveClip(
            SaveClipRequest(LocalDate(2026, 3, 1), "/src.mp4", 0, 1f, null, FilmFormat.CINEMA, DaySource.GALLERY, Instant.fromEpochSeconds(0)),
        )
        val other = projects.create("Garden", FilmFormat.STORY, LocalDate(2026, 4, 1))
        assertEquals(other.id, projects.activeId())
        projects.delete(first)
        assertEquals(listOf(other.id), projects.projects.first().map { it.id })
        assertEquals(false, File(saved.file).exists())
        assertEquals(other.id, projects.activeId())
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun day(date: String, file: String = "days/$date.mp4", look: LookParams = LookParams()) = Day(
        date = LocalDate.parse(date), file = file, thumb = "thumbs/$date.jpg", source = DaySource.CAMERA,
        look = look, takenAt = Instant.fromEpochMilliseconds(1000), createdAt = Instant.fromEpochMilliseconds(2000),
    )

    @Test
    fun insertReplaceDelete() = runTest {
        val d = day("2026-11-10", look = LookParams(LookPreset.SILVER, grain = 40))
        days.save(d)
        assertEquals(d.copy(projectId = projects.activeId(), updatedAt = 5_000L), days.getDay(d.date))
        assertEquals(1, days.observeCount().first())

        days.save(d.copy(file = "days/new.mp4"))
        assertEquals("days/new.mp4", days.observeDay(d.date).first()?.file)
        assertEquals(1, days.observeCount().first())

        days.delete(d.date)
        assertNull(days.getDay(d.date))
        assertEquals(0, days.observeCount().first())
    }

    @Test
    fun editsAreStampedAndDeletesLeaveTombstones() = runTest {
        val d = day("2026-11-10")
        days.save(d)
        clock = 6_000L
        days.save(days.getDay(d.date)!!.copy(driveFileId = "x", backup = BackupState.DONE))
        assertEquals(5_000L, days.getDay(d.date)!!.updatedAt)
        days.save(days.getDay(d.date)!!.copy(file = "days/other.mp4"))
        assertEquals(6_000L, days.getDay(d.date)!!.updatedAt)
        assertEquals(BackupState.PENDING, days.getDay(d.date)!!.backup)

        clock = 7_000L
        days.delete(d.date)
        films.save(Film(id = "f", name = "N"))
        films.delete("f")
        val id = projects.activeId()
        assertEquals(
            setOf(Triple("DAY", id, "2026-11-10"), Triple("FILM", id, "f")),
            tombs.all().map { Triple(it.kind, it.projectId, it.key) }.toSet(),
        )
        assertEquals(7_000L, tombs.all().first().deletedAt)

        projects.rename(id, "Renamed")
        assertEquals(7_000L, projects.projects.first().single().updatedAt)
        days.applyRemote(day("2026-11-11").copy(projectId = id, updatedAt = 42, updatedBy = "other"))
        assertEquals("other", days.getDay(LocalDate.parse("2026-11-11"))!!.updatedBy)
        days.removeRemote(id, LocalDate.parse("2026-11-11"))
        assertEquals(2, tombs.all().size)
    }

    @Test
    fun clipsAreScopedToActiveProject() = runTest {
        val first = projects.activeId()
        days.save(day("2026-11-10"))
        val second = projects.create("Lisbon", FilmFormat.STORY, LocalDate.parse("2026-11-01"))
        assertEquals(second.id, projects.activeId())
        assertNull(days.getDay(LocalDate.parse("2026-11-10")))
        days.save(day("2026-11-10", file = "days/lisbon.mp4"))
        projects.setActive(first)
        assertEquals("days/2026-11-10.mp4", days.getDay(LocalDate.parse("2026-11-10"))?.file)
        assertEquals(1, days.observeCount().first())
    }

    @Test
    fun draftsAreInvisibleToEveryCountingObserver() = runTest {
        days.save(day("2026-11-10"))
        days.save(day("2026-11-09").copy(draft = true, importRunId = "run1", draftCandidates = "[]"))
        assertEquals(listOf("2026-11-10"), days.observeAll().first().map { it.date.toString() })
        assertEquals(1, days.observeCount().first())
        assertEquals(listOf("2026-11-10"), days.observeAllProjects().first().map { it.date.toString() })
        assertEquals(listOf("2026-11-09"), days.observeDrafts().first().map { it.date.toString() })
        assertEquals(true, days.getDay(LocalDate.parse("2026-11-09"))?.draft)
        assertEquals(1, projects.stats.first().values.single().clips)
        assertEquals(1, days.draftsOfRun(projects.activeId(), "run1").size)
        assertEquals(0, days.draftsOfRun(projects.activeId(), "other").size)
    }

    @Test
    fun savingOverADraftConfirmsIt() = runTest {
        val date = LocalDate(2026, 3, 1)
        val request = SaveClipRequest(date, "/src.mp4", 0, 1f, null, FilmFormat.CINEMA, DaySource.GALLERY, Instant.fromEpochSeconds(0))
        days.saveClip(request.copy(draft = true, importRunId = "r", draftCandidates = "[]"))
        assertEquals(0, days.observeCount().first())
        days.saveClip(request)
        assertEquals(1, days.observeCount().first())
        assertEquals(0, days.observeDrafts().first().size)
    }

    @Test
    fun rangeQueryInclusiveAndOrdered() = runTest {
        listOf("2026-11-12", "2026-11-10", "2026-11-11", "2026-11-20").forEach { days.save(day(it)) }
        val r = days.observeRange(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-12")).first()
        assertEquals(listOf("2026-11-10", "2026-11-11", "2026-11-12"), r.map { it.date.toString() })
    }

    @Test
    fun filmCrud() = runTest {
        val f = Film(
            id = "a", name = "Week", rangeType = FilmRangeType.CUSTOM,
            rangeFrom = LocalDate.parse("2026-11-01"), rangeTo = LocalDate.parse("2026-11-07"), look = LookPreset.KIOSK,
        )
        films.save(f)
        assertEquals(f.copy(projectId = projects.activeId(), updatedAt = 5_000L), films.getFilm("a"))
        films.save(f.copy(name = "Renamed"))
        assertEquals(listOf("Renamed"), films.observeAll().first().map { it.name })
        films.delete("a")
        assertEquals(0, films.observeCount().first())
    }

    @Test
    fun settingsDefaultsAndRoundTrip() = runTest {
        val file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        val repo = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        val s = repo.settings.first()
        assertEquals(LookPreset.NATURAL, s.defaultLook)
        assertEquals(FilmFormat.CINEMA, s.newFilmFormat)
        assertEquals(true, s.dateStamp)
        assertEquals(app.sliver.data.settings.Appearance.SYSTEM, s.appearance)
        assertEquals(false, s.saveFullRecordings)
        assertEquals(LocalTime(20, 0), s.reminder.time)
        assertEquals(List(7) { true }, s.reminder.days)
        assertEquals(true, s.notif.filmReady)
        assertEquals(false, s.reminder.enabled)
        assertEquals(true, s.reminder.skipIfDone)
        assertEquals(true, s.account.wifiOnly)
        assertNull(s.firstDay)

        repo.update {
            it.copy(
                firstDay = LocalDate.parse("2026-01-01"),
                appearance = app.sliver.data.settings.Appearance.LIGHT,
                reminder = it.reminder.copy(enabled = true, lastCall = true, projects = setOf("a", "b"), days = List(7) { i -> i < 5 }),
            )
        }
        val u = repo.settings.first()
        assertEquals(true, u.reminder.enabled)
        assertEquals(true, u.reminder.lastCall)
        assertEquals(setOf("a", "b"), u.reminder.projects)
        assertEquals(app.sliver.data.settings.Appearance.LIGHT, u.appearance)
        assertEquals(List(7) { i -> i < 5 }, u.reminder.days)
        assertEquals(LocalDate.parse("2026-01-01"), u.firstDay)
    }
}
