package app.sliver.shots

import app.sliver.core.navigation.Route
import app.sliver.feature.archive.ArchiveSession
import app.sliver.feature.archive.DayDetailScreen
import app.sliver.feature.archive.ProjectScreen
import app.sliver.feature.backfill.BackfillScreen
import app.sliver.feature.backup.BackupScreen
import app.sliver.feature.backup.RestoreScreen
import app.sliver.feature.camera.CameraScreen
import app.sliver.feature.films.ExportScreen
import app.sliver.feature.films.FilmBuilderScreen
import app.sliver.feature.films.FilmNav
import app.sliver.feature.films.FilmPlayerScreen
import app.sliver.feature.films.PremiereScreen
import app.sliver.feature.films.YearInClipsScreen
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
    @Test fun backfill() = shoot("Backfill", inspection = true) { BackfillScreen {} }
    @Test fun filmBuilder() = shoot("FilmBuilder", inspection = true) { FilmBuilderScreen({}, {}) }
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
        shoot("FilmPlayer", darkOnly = true, inspection = true) { FilmPlayerScreen({}, {}) }
    }
    @Test fun year() = shoot("YearInSeconds", darkOnly = true) { YearInClipsScreen({}, {}) }
    @Test fun backup() = shoot("Backup") { BackupScreen({}, {}) }
    @Test fun restore() {
        // Restore asks for sign-in first; the golden shows the signed-in Drive scan result.
        kotlinx.coroutines.runBlocking { settings.update { it.copy(account = it.account.copy(signedIn = true, email = "Demo account")) } }
        shoot("Restore", settled = "Nothing to restore") { RestoreScreen {} }
    }
    @Test fun reminders() = shoot("Notifications") { RemindersScreen {} }
    @Test fun onboarding() = shoot("OnbV2") { OnboardingScreen({}, {}) }
    @Test fun camera() = shoot("CameraV2", darkOnly = true, inspection = true) { CameraScreen({}, {}) }
    @Test fun trim() {
        CaptureSession.path = thumbPath(0)
        shoot("EditClipV2", darkOnly = true, inspection = true) { TrimScreen({}, SavedStateHandle()) }
    }
    @Test fun photoTrim() {
        CaptureSession.path = thumbPath(1)
        CaptureSession.source = app.sliver.data.DaySource.PHOTO
        shoot("PhotoTrim", darkOnly = true, inspection = true) { PhotoTrimScreen {} }
    }
}
