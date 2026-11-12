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

class ProjectRestoreTest {
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
}
