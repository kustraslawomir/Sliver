package app.sliver.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.core.navigation.Route
import app.sliver.data.backup.DriveTrashQueue
import app.sliver.data.backup.RestoredDayWriter
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.InMemoryDriveClient
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
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.gallery.AndroidGalleryProvider
import app.sliver.feature.gallery.loadIntoSession
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class CopyingExporter : ClipExporter {
    val sources = mutableListOf<String>()

    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropY: Float?, format: FilmFormat, videoPath: String, thumbPath: String,
        shortMode: ShortMode, look: LookParams,
    ): ExportedClip {
        sources += source
        File(videoPath).writeText(File(source).readText())
        File(thumbPath).writeText("t")
        return ExportedClip(videoPath, thumbPath, 1000)
    }

    override suspend fun photoToClip(
        source: String, motion: Motion, format: FilmFormat, videoPath: String, thumbPath: String, look: LookParams,
    ): ExportedClip = error("unused")
}

/** The app copies every picked item into its own storage, so moving the originals off the phone must not hurt the project. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class OriginalsDisappearTest {
    private lateinit var ctx: Context
    private lateinit var db: SliverDatabase
    private lateinit var projects: ProjectRepositoryImpl
    private lateinit var days: DayRepositoryImpl
    private lateinit var films: FilmRepositoryImpl
    private lateinit var settings: DataStoreSettingsRepository
    private lateinit var files: AndroidFileStore
    private val exporter = CopyingExporter()
    private val originals = kotlin.io.path.createTempDirectory("originals").toFile()

    @BeforeTest
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        files = AndroidFileStore(ctx.filesDir, ctx.cacheDir)
        settings = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
                File.createTempFile("prefs", ".preferences_pb").apply { delete() }
            },
        )
        projects = ProjectRepositoryImpl(db.projectDao(), settings, db.dayDao(), db.filmDao(), files, DriveTrashQueue {})
        days = DayRepositoryImpl(db.dayDao(), projects, exporter, files)
        films = FilmRepositoryImpl(db.filmDao(), projects)
    }

    @AfterTest
    fun tearDown() {
        db.close()
        originals.deleteRecursively()
    }

    @Test
    fun dayThumbnailFilmInputAndBackupSurviveDeletingTheOriginal() = runTest {
        val original = File(originals, "IMG_0001.mov").apply { writeText("clip-bytes") }
        val provider = AndroidGalleryProvider(ctx, files)
        val date = LocalDate(2026, 11, 10)

        val route = loadIntoSession(provider, files, Uri.fromFile(original).toString(), date)
        assertNotNull(route)
        val tmp = assertNotNull(CaptureSession.path)
        assertTrue(File(tmp).startsWith(ctx.cacheDir))

        val saved = days.saveClip(SaveClipRequest(date, tmp, 0, 1f, null, FilmFormat.CINEMA, DaySource.GALLERY, Instant.fromEpochSeconds(0)))
        assertTrue(original.delete())
        File(tmp).delete()

        assertEquals("clip-bytes", File(saved.file).readText())
        assertTrue(File(saved.file).startsWith(ctx.filesDir))
        assertTrue(File(saved.thumb).exists())
        assertFalse(saved.toString().contains(originals.path))
        assertEquals(saved.file, days.getDay(date)?.file)
        assertEquals(listOf(tmp), exporter.sources)

        val drive = InMemoryDriveClient()
        val engine = SyncEngine(
            drive, days, films, settings, DayFileReader { p -> File(p).takeIf { it.exists() }?.readBytes() }, projects, files,
            RestoredDayWriter { _, _, _ -> 0 },
        )
        assertEquals(null, engine.run())
        assertEquals(BackupState.DONE, days.getDay(date)?.backup)
        assertEquals(1, days.observeAll().first().size)
    }

    @Test
    fun livePhotoPairBecomesVideoAndSwitchesBackToThePhoto() = runTest {
        val still = File(originals, "IMG_0002.jpg").apply { writeText("still") }
        val mov = File(originals, "IMG_0002.mov").apply { writeText("motion") }
        val provider = AndroidGalleryProvider(ctx, files)
        val stillUri = Uri.fromFile(still).toString()
        val movUri = Uri.fromFile(mov).toString()

        assertEquals(Route.Trim, loadIntoSession(provider, files, stillUri, null, liveVideo = movUri))
        assertEquals("motion", File(assertNotNull(CaptureSession.path)).readText())
        assertTrue(CaptureSession.motionPhoto)
        assertEquals(movUri, CaptureSession.liveVideoUri)

        assertEquals(Route.PhotoTrim, loadIntoSession(provider, files, stillUri, null, asVideo = false, liveVideo = movUri))
        assertEquals("still", File(assertNotNull(CaptureSession.path)).readText())
        assertEquals(DaySource.PHOTO, CaptureSession.source)

        assertEquals(Route.Trim, loadIntoSession(provider, files, stillUri, null, asVideo = true, liveVideo = movUri))
    }
}
