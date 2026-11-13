package app.sliver.data

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.ExportedClip
import app.sliver.data.export.ShortMode
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.ProjectRepositoryImpl
import app.sliver.data.repo.SaveClipRequest
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.feature.autoimport.AutoImportExecutor
import app.sliver.feature.autoimport.AutoImportStore
import app.sliver.feature.autoimport.ImportRun
import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.autoimport.MediaScanner
import app.sliver.feature.autoimport.decodeCandidates
import app.sliver.feature.autoimport.dismissTokens
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.MotionClip
import app.sliver.feature.trim.TrimMedia
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class Recording : ClipExporter {
    data class Call(val source: String, val startMs: Long, val photo: Boolean)

    val calls = mutableListOf<Call>()

    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropX: Float?, cropY: Float?, format: FilmFormat, videoPath: String, thumbPath: String,
        shortMode: ShortMode, look: LookParams,
    ): ExportedClip {
        calls += Call(source, startMs, false)
        File(videoPath).writeText("v")
        File(thumbPath).writeText("t")
        return ExportedClip(videoPath, thumbPath, 1000)
    }

    override suspend fun photoToClip(
        source: String, motion: Motion, format: FilmFormat, videoPath: String, thumbPath: String, look: LookParams, cropX: Float?, cropY: Float?
    ): ExportedClip {
        calls += Call(source, 0, true)
        File(videoPath).writeText("v")
        File(thumbPath).writeText("t")
        return ExportedClip(videoPath, thumbPath, 1000)
    }
}

private class FakeGallery(private val dir: File) : GalleryProvider {
    override suspend fun isVideo(uri: String) = uri.contains("video")
    override suspend fun describe(uris: List<String>): List<GalleryItem> = emptyList()
    override suspend fun copyToTmp(uri: String): String = File(dir, "rec_${uri.hashCode()}.tmp").apply { writeText("x") }.path
    override suspend fun keep(uri: String): String = copyToTmp(uri)
    override suspend fun extractMotionVideo(uri: String): MotionClip? =
        if (uri.contains("motion")) MotionClip(File(dir, "rec_motion.mp4").apply { writeText("m") }.path, 6_000_000) else null
}

private object FakeMedia : TrimMedia {
    override suspend fun durationMs(path: String) = 10_000L
    override suspend fun aspect(path: String) = 1.78f
    override suspend fun autoPickMs(path: String, durationMs: Long) = 0L
    override suspend fun fileSizeBytes(path: String) = 1L
    override suspend fun thumbnails(path: String, durationMs: Long, count: Int, onEach: (ImageBitmap) -> Unit) = emptyList<ImageBitmap>()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class AutoImportExecutorTest {
    private lateinit var db: SliverDatabase
    private lateinit var days: DayRepositoryImpl
    private lateinit var projects: ProjectRepositoryImpl
    private lateinit var store: AutoImportStore
    private lateinit var exporter: Recording
    private lateinit var executor: AutoImportExecutor
    private var entries = emptyList<MediaEntry>()

    private fun ms(day: Int, hour: Int = 12) = Instant.parse("2026-11-%02dT%02d:00:00Z".format(day, hour)).toEpochMilliseconds()

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        val files = AndroidFileStore(ctx.filesDir, ctx.cacheDir)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File.createTempFile("prefs", ".preferences_pb").apply { delete() } })
        projects = ProjectRepositoryImpl(db.projectDao(), settings, db.dayDao(), db.filmDao(), files, app.sliver.data.backup.DriveTrashQueue {})
        exporter = Recording()
        days = DayRepositoryImpl(db.dayDao(), projects, exporter, files)
        store = AutoImportStore(PreferenceDataStoreFactory.create(scope = scope) { File.createTempFile("run", ".preferences_pb").apply { delete() } })
        val scanner = object : MediaScanner {
            override suspend fun scan(from: LocalDate, to: LocalDate, zone: TimeZone, sniffDays: Set<LocalDate>, onFound: (Int) -> Unit) = entries
        }
        executor = AutoImportExecutor(
            store, days, projects, scanner, FakeGallery(ctx.cacheDir), FakeMedia, files,
            today = { LocalDate(2026, 11, 12) }, zone = { TimeZone.UTC },
        )
    }

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun startRun(from: String = "2026-11-01"): ImportRun =
        ImportRun("run1", projects.activeId(), from).also { store.start(it) }

    @Test
    fun draftsGoOnlyOnGapDaysAndFilledDaysAreUntouched() = runTest {
        val project = projects.activeId()
        days.saveClip(SaveClipRequest(LocalDate(2026, 11, 3), "/x.mp4", 0, 1f, null, FilmFormat.CINEMA, DaySource.CAMERA, Instant.fromEpochSeconds(0)))
        entries = listOf(
            MediaEntry("content://video/1", true, ms(2), 12_000),
            MediaEntry("content://video/2", true, ms(2, 15), 3_000),
            MediaEntry("content://image/3", false, ms(3)),
            MediaEntry("content://image/4", false, ms(4)),
            MediaEntry("content://video/future", true, ms(12), 5_000),
        )
        startRun()
        executor.run()

        val drafts = days.observeDrafts().first()
        assertEquals(listOf(2, 4), drafts.map { it.date.day })
        assertTrue(drafts.all { it.draft && it.importRunId == "run1" && it.projectId == project })
        assertEquals(DaySource.GALLERY, drafts[0].source)
        assertEquals(DaySource.PHOTO, drafts[1].source)
        assertEquals(listOf("content://video/1", "content://video/2"), decodeCandidates(drafts[0].draftCandidates).map { it.uri })
        assertEquals(false, days.getDay(LocalDate(2026, 11, 3))!!.draft)
        assertEquals(listOf(3), days.observeAll().first().map { it.date.day })
        assertEquals(4_500, exporter.calls.first { it.source != "/x.mp4" && !it.photo }.startMs)
        assertTrue(exporter.calls.last().photo)
        assertEquals(true, store.current()!!.finished)
        assertEquals(2, store.current()!!.done)
    }

    @Test
    fun motionPhotoIsCutAroundItsPresentationTimestamp() = runTest {
        entries = listOf(MediaEntry("content://image/motion1", false, ms(5), 0, motion = true))
        startRun()
        executor.run()
        assertEquals(DaySource.GALLERY, days.observeDrafts().first().single().source)
        assertEquals(5_500L, exporter.calls.single().startMs)
        assertEquals(false, exporter.calls.single().photo)
    }

    @Test
    fun runningAgainDoesNotDuplicateOrRedo() = runTest {
        entries = listOf(MediaEntry("content://image/1", false, ms(5)))
        startRun()
        executor.run()
        store.update("run1") { it.copy(finished = false) }
        executor.run()
        assertEquals(1, exporter.calls.size)
        assertEquals(1, days.observeDrafts().first().size)
    }

    @Test
    fun undoAllRemovesOnlyDraftsOfThatRunAndNotKeptOnes() = runTest {
        entries = listOf(
            MediaEntry("content://image/1", false, ms(5)),
            MediaEntry("content://image/2", false, ms(6)),
        )
        startRun()
        executor.run()
        // The user keeps the 5th: it becomes a normal day and must survive Undo all.
        val kept = days.getDay(LocalDate(2026, 11, 5))!!
        days.save(kept.copy(draft = false, importRunId = null, draftCandidates = null))
        val sixth = days.getDay(LocalDate(2026, 11, 6))!!

        executor.undoAll()

        assertEquals(0, days.observeDrafts().first().size)
        assertEquals(null, days.getDay(LocalDate(2026, 11, 6)))
        assertEquals(false, File(sixth.file).exists())
        assertEquals(listOf(5), days.observeAll().first().map { it.date.day })
        assertEquals(null, store.current())
    }

    @Test
    fun keepWithoutReviewSavesNormalDaysAndUndoAllStillRemovesThem() = runTest {
        entries = listOf(
            MediaEntry("content://video/1", true, ms(5), 8_000),
            MediaEntry("content://image/2", false, ms(6)),
        )
        store.start(ImportRun("run1", projects.activeId(), "2026-11-01", keep = true))
        executor.run()

        assertEquals(0, days.observeDrafts().first().size)
        assertEquals(listOf(5, 6), days.observeAll().first().map { it.date.day })
        assertEquals("2026-11-05", store.current()!!.firstFilled)
        assertEquals("2026-11-06", store.current()!!.lastFilled)
        assertEquals(1, store.current()!!.videos)
        assertEquals(1, store.current()!!.photos)

        executor.undoAll()
        assertEquals(emptyList(), days.observeAll().first())
    }

    @Test
    fun removedDayComesBackOnlyWithNewFiles() = runTest {
        entries = listOf(MediaEntry("content://video/old", true, ms(5), 8_000))
        store.start(ImportRun("run1", projects.activeId(), "2026-11-01", keep = true))
        executor.run()
        val day = days.getDay(LocalDate(2026, 11, 5))!!

        // The user removes the day: its files are remembered, the row goes.
        store.dismiss(day.projectId, day.date, decodeCandidates(day.draftCandidates).map { it.uri })
        days.deleteRow(day.projectId, day.date)

        store.start(ImportRun("run2", projects.activeId(), "2026-11-01", keep = true))
        executor.run()
        assertEquals(null, days.getDay(LocalDate(2026, 11, 5)))

        // A new file for that day appears: the day is filled from it, the old one stays as an extra option.
        entries = entries + MediaEntry("content://image/new", false, ms(5, 18))
        store.start(ImportRun("run3", projects.activeId(), "2026-11-01", keep = true))
        executor.run()
        val again = days.getDay(LocalDate(2026, 11, 5))!!
        assertEquals(listOf("content://image/new", "content://video/old"), decodeCandidates(again.draftCandidates).map { it.uri })
        assertTrue(exporter.calls.last().photo)
    }

    @Test
    fun rejectedItemsComeBackOnlyWhenAsked() = runTest {
        entries = listOf(MediaEntry("content://video/old", true, ms(5), 8_000))
        store.start(ImportRun("run1", projects.activeId(), "2026-11-01"))
        executor.run()
        val day = days.getDay(LocalDate(2026, 11, 5))!!
        store.dismiss(day.projectId, day.date, decodeCandidates(day.draftCandidates).flatMap(::dismissTokens))
        days.deleteRow(day.projectId, day.date)

        store.start(ImportRun("run2", projects.activeId(), "2026-11-01"))
        executor.run()
        assertEquals(null, days.getDay(LocalDate(2026, 11, 5)))

        store.start(ImportRun("run3", projects.activeId(), "2026-11-01", includeRejected = true))
        executor.run()
        assertEquals("run3", days.getDay(LocalDate(2026, 11, 5))?.importRunId)
        // Picked again, so the launch cleanup keeps it.
        assertEquals(0, executor.pruneDismissedDrafts())
        assertTrue(days.getDay(LocalDate(2026, 11, 5)) != null)
    }
}
