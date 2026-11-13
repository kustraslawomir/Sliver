package app.sliver.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.core.ui.UndoToasts
import app.sliver.data.backup.TombstoneKind
import app.sliver.data.backup.TombstoneStore
import app.sliver.data.db.Day
import app.sliver.data.db.Project
import app.sliver.data.db.SliverDatabase
import app.sliver.data.db.Tombstone
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.ExportedClip
import app.sliver.data.export.ShortMode
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.repo.ProjectRepositoryImpl
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.feature.autoimport.DraftActions
import app.sliver.feature.autoimport.DraftCandidate
import app.sliver.feature.autoimport.encodeCandidates
import app.sliver.feature.autoimport.isAuto
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.MotionClip
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

private class Tombs : TombstoneStore {
    val list = mutableListOf<Tombstone>()
    override suspend fun record(kind: String, projectId: String, key: String, at: Long) { list += Tombstone(kind, projectId, key, at) }
    override suspend fun all() = list.toList()
    override suspend fun clear(kind: String, projectId: String, key: String) { list.removeAll { it.kind == kind && it.key == key } }
}

private object NoExporter : ClipExporter {
    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropX: Float?, cropY: Float?, format: FilmFormat, videoPath: String, thumbPath: String,
        shortMode: ShortMode, look: LookParams,
    ): ExportedClip = error("unused")

    override suspend fun photoToClip(
        source: String, motion: Motion, format: FilmFormat, videoPath: String, thumbPath: String, look: LookParams, cropX: Float?, cropY: Float?
    ): ExportedClip = error("unused")
}

private object NoGallery : GalleryProvider {
    override suspend fun isVideo(uri: String) = false
    override suspend fun describe(uris: List<String>): List<GalleryItem> = emptyList()
    override suspend fun copyToTmp(uri: String): String = error("unused")
    override suspend fun keep(uri: String): String = error("unused")
    override suspend fun extractMotionVideo(uri: String): MotionClip? = null
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class AutoReviewTest {
    private lateinit var db: SliverDatabase
    private lateinit var days: DayRepositoryImpl
    private lateinit var projects: ProjectRepositoryImpl
    private lateinit var actions: DraftActions
    private lateinit var dir: File
    private val tombs = Tombs()

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        val files = AndroidFileStore(ctx.filesDir, ctx.cacheDir)
        dir = File(ctx.cacheDir, "review").apply { mkdirs() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File.createTempFile("prefs", ".preferences_pb").apply { delete() } })
        projects = ProjectRepositoryImpl(db.projectDao(), settings, db.dayDao(), db.filmDao(), files, app.sliver.data.backup.DriveTrashQueue {})
        days = DayRepositoryImpl(db.dayDao(), projects, NoExporter, files, tombstones = tombs)
        actions = DraftActions(days, FilmRepositoryImpl(db.filmDao(), projects), files, NoGallery)
        UndoToasts.current.value?.let { UndoToasts.dismiss(it) }
    }

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun put(
        day: Int, project: String? = null, draft: Boolean = false, run: String? = null, items: Int = 3,
    ): Day {
        val pid = project ?: projects.activeId()
        val d = LocalDate(2026, 11, day)
        val video = File(dir, "$pid-$day.mp4").apply { writeText("v") }
        val thumb = File(dir, "$pid-$day.jpg").apply { writeText("t") }
        val row = Day(
            date = d, file = video.path, thumb = thumb.path, source = DaySource.GALLERY, takenAt = Instant.fromEpochSeconds(0),
            createdAt = Instant.fromEpochSeconds(0), projectId = pid, draft = draft, importRunId = run,
            draftCandidates = run?.let { encodeCandidates(List(items) { DraftCandidate("content://v/$day-$it", true, 3000) }) },
        )
        days.save(row)
        return row
    }

    private suspend fun waitFor(check: suspend () -> Boolean) {
        repeat(100) { if (check()) return; Thread.sleep(50) }
        assertTrue(check())
    }

    @Test
    fun autoDayPredicate() = runTest {
        assertTrue(put(1, run = "r").isAuto())
        assertTrue(!put(2, draft = true, run = "r").isAuto())
        assertTrue(!put(3).isAuto())
    }

    @Test
    fun keepAllMarksDraftsAndAutoDaysReviewedAndLeavesTheRest() = runTest {
        put(1)
        put(2, run = "r")
        put(3, draft = true, run = "r")
        assertEquals(2, actions.keepAll())

        val left = days.observeAll().first()
        assertEquals(listOf(1, 2, 3), left.map { it.date.day })
        assertTrue(left.none { it.isAuto() || it.draft || it.draftCandidates != null })
        assertEquals(0, actions.drafts.first().size)
    }

    @Test
    fun removeUncheckedDeletesOnlyAutoDaysOfTheActiveProjectWithTombstonesAndUndoRestores() = runTest {
        val active = projects.activeId()
        db.projectDao().upsert(Project("other", "Other", startDate = LocalDate(2026, 11, 1), createdAt = Instant.fromEpochSeconds(0)))
        val camera = put(1)
        val auto2 = put(2, run = "r")
        val auto3 = put(3, run = "r")
        put(4, draft = true, run = "r")
        val reviewed = put(5).let { days.save(it.copy(importRunId = null)); it }
        put(6, project = "other", run = "r")

        assertEquals(2, actions.removeUnchecked())

        assertEquals(listOf(1, 5), days.observeAll().first().map { it.date.day })
        assertEquals(listOf(4), days.observeDrafts().first().map { it.date.day })
        assertNotNull(days.getDay("other", LocalDate(2026, 11, 6)))
        assertEquals(setOf("2026-11-02", "2026-11-03"), tombs.list.filter { it.kind == TombstoneKind.DAY && it.projectId == active }.map { it.key }.toSet())
        assertEquals("2 days removed", UndoToasts.current.value!!.message)
        assertTrue(!File(auto2.thumb).exists() && File(camera.file).exists() && File(reviewed.file).exists())

        UndoToasts.current.value!!.onUndo()
        waitFor { days.observeAll().first().size == 4 }
        waitFor { File(auto3.file).exists() && File(auto2.thumb).exists() }
        assertTrue(days.getDay(LocalDate(2026, 11, 3))!!.isAuto())
    }

    @Test
    fun keptDayLeavesTheCheckListAndSkippingAConfirmedDayLeavesATombstone() = runTest {
        val a = put(2, run = "r")
        put(3, run = "r")
        assertEquals(1, actions.keepDays(listOf(a)))
        assertEquals(listOf(3), actions.drafts.first().map { it.date.day })

        actions.skip(days.getDay(LocalDate(2026, 11, 3))!!)
        assertNull(days.getDay(LocalDate(2026, 11, 3)))
        assertEquals(listOf("2026-11-03"), tombs.list.map { it.key })
    }
}
