package app.sliver.shots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.data.db.Day
import app.sliver.feature.archive.ProjectScreen
import app.sliver.feature.autoimport.DraftCandidate
import app.sliver.feature.autoimport.ReviewSheetBody
import app.sliver.feature.autoimport.encodeCandidates
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Goldens for T-autofill-review: auto-filled days in the calendar, the day media sheet and the chip sheet (synthetic data). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi", application = android.app.Application::class)
class AutoFillShots : ShotTest() {
    private val items = mapOf(3 to 4, 4 to 1, 5 to 3, 7 to 12)

    private fun candidates(n: Int) = encodeCandidates(
        List(n) { i -> DraftCandidate("content://synthetic/$i", video = i % 3 != 2, durationMs = 4_000L + i * 1_000, motion = i == 4) },
    )

    private fun auto(day: Int, n: Int): Day =
        runBlockingDay(day).copy(importRunId = "run", draftCandidates = candidates(n))

    private fun runBlockingDay(day: Int): Day =
        kotlinx.coroutines.runBlocking { db.dayDao().getByDate(ShotProjectId, LocalDate(2026, 11, day))!! }

    override suspend fun seed() {
        super.seed()
        items.forEach { (d, n) -> db.dayDao().upsert(auto(d, n)) }
        db.dayDao().upsert(runBlockingDay(8).copy(draft = true, importRunId = "run", draftCandidates = candidates(2)))
    }

    @Test fun calendar() = shoot("AutoFillCalendar") { ProjectScreen {} }

    @Test fun chipSheet() = shoot("AutoFillChipSheet", inspection = true) {
        Column(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
            Column(Modifier.background(SliverColors.Surface1).padding(20.dp)) {
                ReviewSheetBody(drafts = 0, auto = 3, busy = false, onKeepAll = {}, onRemoveUnchecked = {}, onWalk = {}, days = listOf(3, 6, 7).map { auto(it, 2) })
            }
        }
    }

    private fun withRun(run: app.sliver.feature.autoimport.ImportRun) = kotlinx.coroutines.runBlocking {
        org.koin.mp.KoinPlatform.getKoin().get<app.sliver.feature.autoimport.AutoImportStore>().start(run)
    }

    @Test fun fillProgress() {
        withRun(
            app.sliver.feature.autoimport.ImportRun(
                id = "run", projectId = ShotProjectId, from = "2026-06-21", done = 64, total = 143, keep = true, gapDays = 155,
                scanned = 1204, videos = 31, photos = 21, skipped = 3, current = "2026-08-22", firstFilled = "2026-06-21", lastFilled = "2026-08-22",
            ),
        )
        shoot("FillCalendarProgress", inspection = true) { app.sliver.feature.autoimport.AutoImportScreen({}, {}) }
    }

    @Test fun fillDone() {
        withRun(
            app.sliver.feature.autoimport.ImportRun(
                id = "run", projectId = ShotProjectId, from = "2026-06-21", done = 4, total = 4, finished = true, keep = true, gapDays = 6,
                scanned = 40, videos = 3, photos = 1, firstFilled = "2026-11-03", lastFilled = "2026-11-08",
            ),
        )
        shoot("FillCalendarDone", inspection = true) { app.sliver.feature.autoimport.AutoImportScreen({}, {}) }
    }
}
