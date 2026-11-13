package app.sliver.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import app.sliver.data.files.TmpRecording
import app.sliver.feature.trim.OriginalRescue
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.isOriginal
import kotlin.test.assertNull
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

private class KeepExporter : ClipExporter {
    val sources = mutableListOf<String>()

    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropX: Float?, cropY: Float?, format: FilmFormat, videoPath: String, thumbPath: String,
        shortMode: ShortMode, look: LookParams,
    ): ExportedClip {
        sources += source
        File(videoPath).writeText(File(source).readText())
        File(thumbPath).writeText("t")
        return ExportedClip(videoPath, thumbPath, 1000)
    }

    override suspend fun photoToClip(
        source: String, motion: Motion, format: FilmFormat, videoPath: String, thumbPath: String, look: LookParams, cropX: Float?, cropY: Float?
    ): ExportedClip = error("unused")
}

/** T-glass-v1.7 10: in-app recordings are kept for Change moment; 10.5: ones still in tmp are rescued at launch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class OriginalsKeptTest {
    private lateinit var ctx: Context
    private lateinit var db: SliverDatabase
    private lateinit var projects: ProjectRepositoryImpl
    private lateinit var days: DayRepositoryImpl
    private lateinit var files: AndroidFileStore
    private val exporter = KeepExporter()
    private val date = LocalDate(2026, 11, 13)

    @BeforeTest
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        File(ctx.cacheDir, "tmp").deleteRecursively()
        File(ctx.filesDir, "originals").deleteRecursively()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        files = AndroidFileStore(ctx.filesDir, ctx.cacheDir)
        val settings = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
                File.createTempFile("prefs", ".preferences_pb").apply { delete() }
            },
        )
        projects = ProjectRepositoryImpl(db.projectDao(), settings, db.dayDao(), db.filmDao(), files, DriveTrashQueue {})
        days = DayRepositoryImpl(db.dayDao(), projects, exporter, files)
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun record(start: Long, text: String = "full-recording") = File(files.newTmpPath(start)).apply { writeText(text) }

    private fun request(source: String, startMs: Long, at: Long) =
        SaveClipRequest(date, source, startMs, 1f, null, FilmFormat.CINEMA, DaySource.CAMERA, Instant.fromEpochMilliseconds(at))

    @Test
    fun savingACameraClipKeepsTheRecordingAndChangeMomentReusesIt() = runTest {
        val rec = record(1_000)
        val day = days.saveClip(request(rec.path, 2_500, 5_000))
        val kept = assertNotNull(day.original)
        assertTrue(isOriginal(kept))
        assertTrue(File(kept).startsWith(File(ctx.filesDir, "originals")))
        assertEquals("full-recording", File(kept).readText())
        assertFalse(rec.exists())
        assertEquals(2_500, day.originalStartMs)

        // Change moment: same recording, new start; the recording is untouched and stays the day's original.
        val again = days.saveClip(request(kept, 7_000, 5_000))
        assertEquals(kept, again.original)
        assertEquals(7_000, again.originalStartMs)
        assertTrue(File(kept).exists())
        assertEquals(listOf(rec.path, kept), exporter.sources)
    }

    @Test
    fun galleryAndPhotoDaysKeepNoOriginal() = runTest {
        val rec = record(1_000)
        val day = days.saveClip(request(rec.path, 0, 5_000).copy(daySource = DaySource.GALLERY))
        assertNull(day.original)
    }

    @Test
    fun removingADayDeletesItsOriginal() = runTest {
        val day = days.saveClip(request(record(1_000).path, 0, 5_000))
        val undo = SaveUndo(days, files, FilmRepositoryImpl(db.filmDao(), projects))
        undo.remove(day)
        assertFalse(File(assertNotNull(day.original)).exists())
    }

    @Test
    fun rescueAdoptsTodaysRecordingStillInTmpAndLeavesParkedClipsAlone() = runTest {
        // A camera day saved by an older build: the clip exists, the recording was left in tmp.
        val rec = record(10_000)
        rec.setLastModified(12_000)
        val parked = record(20_001, "parked").also { File(it.path + ".jpg").writeText("t") }
        val clip = days.saveClip(request(record(9_000, "other").path, 0, 20_000).copy(daySource = DaySource.GALLERY))
        days.applyRemote(clip.copy(source = DaySource.CAMERA))
        assertEquals(listOf(rec.path, File(files.newTmpPath(9_000)).path).sorted(), files.tmpRecordings().map(TmpRecording::path).sorted())

        assertEquals(1, OriginalRescue(days, files).run())
        val day = assertNotNull(days.getDay(date))
        assertEquals("full-recording", File(assertNotNull(day.original)).readText())
        assertFalse(rec.exists())
        assertTrue(parked.exists())
        assertEquals(0, OriginalRescue(days, files).run())
    }
}
