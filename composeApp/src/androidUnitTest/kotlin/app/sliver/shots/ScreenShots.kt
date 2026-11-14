package app.sliver.shots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.sliver.data.info
import app.sliver.core.navigation.Route
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.archive.DayDetailScreen
import app.sliver.feature.archive.ProjectScreen
import app.sliver.feature.autoimport.AutoImportScreen
import app.sliver.feature.backup.BackupScreen
import app.sliver.feature.backup.RestoreScreen
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.films.ExportScreen
import app.sliver.feature.films.FilmBuilderScreen
import app.sliver.feature.films.FilmNav
import app.sliver.feature.films.FilmPlayerScreen
import app.sliver.feature.films.PremiereScreen
import app.sliver.feature.notifications.RemindersScreen
import app.sliver.feature.onboarding.OnboardingScreen
import app.sliver.feature.settings.SettingsScreen
import app.sliver.feature.trim.PhotoTrimScreen
import app.sliver.feature.trim.TrimScreen
import androidx.lifecycle.SavedStateHandle
import app.sliver.feature.camera.CaptureSession
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xhdpi", application = android.app.Application::class)
class ScreenShots : ShotTest() {
    @Test fun settings() = shoot("Settings") { SettingsScreen({}, {}) }
    @Test fun project() = shoot("ProjectHub") { ProjectScreen {} }
    @Test fun dayDetail() {
        ArchiveSession.selected = kotlinx.datetime.LocalDate(2026, 11, 9)
        shoot("DayDetail", inspection = true) { DayDetailScreen({}, {}) }
    }
    @Test fun fromThisDayEmpty() {
        app.sliver.feature.camera.CaptureSession.forDate = kotlinx.datetime.LocalDate(2026, 11, 3)
        shoot("FromThisDayEmpty", inspection = true) { app.sliver.feature.gallery.DayGalleryScreen({}, {}) }
    }
    @Test fun projectUndo() = shoot("ProjectHubUndo") { UndoOver(app.sliver.core.ui.UndoBarBottomDefault, "3 days removed") { ProjectScreen {} } }
    @Test fun dayDetailUndo() {
        ArchiveSession.selected = kotlinx.datetime.LocalDate(2026, 11, 9)
        shoot("DayDetailUndo", inspection = true) { UndoOver(196.dp, "Clip removed") { DayDetailScreen({}, {}) } }
    }

    /** The undo bar where [app.sliver.core.ui.UndoToastHost] puts it (the host itself times out under the test clock). */
    @androidx.compose.runtime.Composable
    private fun UndoOver(bottom: androidx.compose.ui.unit.Dp, message: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            content()
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = bottom),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
            ) { app.sliver.core.ui.UndoBar(message, {}) }
        }
    }
    @Test fun autoImport() = shoot("AutoImport", inspection = true) { AutoImportScreen({}, {}) }
    @Test fun filmBuilder() = shoot("FilmBuilder", inspection = true) { FilmBuilderScreen({}, {}) }
    @Test fun filterPicker() {
        val frame = android.graphics.Bitmap.createBitmap(90, 120, android.graphics.Bitmap.Config.ARGB_8888).also { b ->
            android.graphics.Canvas(b).drawRect(0f, 0f, 90f, 120f, android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, 0f, 120f, 0xFFB58A6A.toInt(), 0xFF2A3A4A.toInt(), android.graphics.Shader.TileMode.CLAMP)
            })
        }
        val image = frame.asImageBitmap()
        val thumbs = app.sliver.data.FilterPresets.associateWith { p ->
            val m = p.info.model
            val px = IntArray(90 * 120).also { frame.getPixels(it, 0, 90, 0, 0, 90, 120) }
            val out = app.sliver.data.look.LookRaster.render(
                px, 90, 120, if (m.hasLut) app.sliver.data.look.LutGenerator.table(m) else null, 1f,
                app.sliver.data.look.lookFrame(m, 1f, m.grain.toFloat(), 7, 0, 120, animate = false),
            )
            android.graphics.Bitmap.createBitmap(out, 90, 120, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        shoot("FilterPicker") {
            androidx.compose.foundation.layout.Box(Modifier.background(app.sliver.core.theme.SliverColors.BgBase).padding(16.dp)) {
                app.sliver.feature.films.FilmFilterSection(
                    image, app.sliver.data.LookPreset.KIOSK, 80, null, {}, {}, {}, thumbsOverride = thumbs,
                )
            }
        }
    }
    @Test fun export() {
        FilmNav.id = "shot-film"
        shoot("Export", darkOnly = true, inspection = true) { ExportScreen({}, {}) }
    }
    @Test fun premiere() {
        FilmNav.id = "shot-film"
        shoot("ExportPremiere", darkOnly = true, inspection = true) { PremiereScreen {} }
    }
    @Test fun filmPlayer() {
        FilmNav.id = "shot-film"
        kotlinx.coroutines.runBlocking { db.filmDao().upsert(db.filmDao().getById("shot-film")!!.copy(renderPath = thumbPath(0))) }
        shoot("FilmPlayer", darkOnly = true, inspection = true) { FilmPlayerScreen({}, {}) }
    }
    @Test fun backup() = shoot("Backup") { BackupScreen({}, {}) }
    @Test fun restore() {
        // Restore asks for sign-in first; the golden shows the signed-in Drive scan result.
        kotlinx.coroutines.runBlocking { settings.update { it.copy(account = it.account.copy(signedIn = true, email = "Demo account")) } }
        shoot("Restore", settled = "Nothing to restore") { RestoreScreen {} }
    }
    @Test fun reminders() = shoot("Notifications") { RemindersScreen {} }
    @Test fun onboarding() = shoot("OnbV2") { OnboardingScreen({}, {}) }
    @Test fun camera() = shoot("CameraV2", darkOnly = true, inspection = true) { CameraScreen({}, {}) }
    /** T-record-button-v3: photo, video idle and recording (0.6 s) over a bright and a dark scene. */
    @Test fun recordButtonV3() = shoot("RecordButtonV3", darkOnly = true) {
        androidx.compose.foundation.layout.Column {
            listOf(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFF4EFE6), androidx.compose.ui.graphics.Color(0xFFE9935A))), // lint-ok: test scene
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF40283A), androidx.compose.ui.graphics.Color(0xFF141218))), // lint-ok: test scene
            ).forEach { bg ->
                androidx.compose.foundation.layout.Row(
                    Modifier.background(bg).padding(horizontal = 12.dp, vertical = 70.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp),
                ) {
                    app.sliver.feature.camera.RecordButton(app.sliver.feature.camera.RecordState.Photo, false, true, {}, {}, {})
                    app.sliver.feature.camera.RecordButton(app.sliver.feature.camera.RecordState.VideoIdle, false, true, {}, {}, {})
                    app.sliver.feature.camera.RecordButton(app.sliver.feature.camera.RecordState.Recording, false, true, {}, {}, {}, elapsedMs = 600)
                }
            }
        }
    }
    @Test fun trim() {
        CaptureSession.path = thumbPath(0)
        shoot("EditClipV2", darkOnly = true, inspection = true) { TrimScreen({}, SavedStateHandle()) }
    }
    @Test fun editClipDraft() {
        CaptureSession.path = thumbPath(0)
        val cands = List(5) { app.sliver.feature.autoimport.DraftCandidate("content://synthetic/$it", video = it != 2, durationMs = 14_000L) }
        val day = kotlinx.coroutines.runBlocking { db.dayDao().getByDate(ShotProjectId, kotlinx.datetime.LocalDate(2026, 11, 3))!! }
            .copy(importRunId = "run", draftCandidates = app.sliver.feature.autoimport.encodeCandidates(cands))
        kotlinx.coroutines.runBlocking { db.dayDao().upsert(day) }
        CaptureSession.draft = app.sliver.feature.autoimport.DraftContext(day, cands, cands[0].uri, chain = true)
        try {
            shoot("EditClipDraft", darkOnly = true, inspection = true) { TrimScreen({}, SavedStateHandle()) }
        } finally {
            CaptureSession.draft = null
        }
    }
    @Test fun photoTrim() {
        CaptureSession.path = thumbPath(1)
        CaptureSession.source = app.sliver.data.DaySource.PHOTO
        shoot("PhotoTrim", darkOnly = true, inspection = true) { PhotoTrimScreen {} }
    }
}
