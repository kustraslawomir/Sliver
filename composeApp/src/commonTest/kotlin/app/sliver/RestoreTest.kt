package app.sliver

import app.sliver.data.DaySource
import app.sliver.data.FilmRangeType
import app.sliver.data.LookParams
import app.sliver.data.LookPreset
import app.sliver.data.backup.DriveFile
import app.sliver.data.backup.RestoreScan
import app.sliver.data.backup.applyManifestSettings
import app.sliver.data.backup.parseManifestFilms
import app.sliver.data.backup.parseRestoreItem
import app.sliver.data.db.Film
import app.sliver.data.settings.AppSettings
import app.sliver.data.backup.buildManifest
import app.sliver.feature.backup.RestoreStage
import app.sliver.feature.backup.foundCopy
import app.sliver.feature.backup.monthCells
import app.sliver.feature.backup.restoreStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class RestoreTest {
    @Test
    fun parsesDayProperties() {
        val look = LookParams(preset = LookPreset.SILVER, grain = 20)
        val file = DriveFile(
            "f1", "2025-03-04.mp4",
            mapOf("sliverDate" to "2025-03-04", "look" to Json.encodeToString(LookParams.serializer(), look), "source" to "GALLERY"),
        )
        val item = parseRestoreItem(file)!!
        assertEquals(LocalDate(2025, 3, 4), item.date)
        assertEquals(look, item.look)
        assertEquals(DaySource.GALLERY, item.source)
    }

    @Test
    fun ignoresFilesWithoutDate() {
        assertNull(parseRestoreItem(DriveFile("m", "manifest.json")))
        assertNull(parseRestoreItem(DriveFile("x", "x.mp4", mapOf("sliverDate" to "nope"))))
    }

    @Test
    fun manifestRoundTripsFilmsAndSettings() {
        val film = Film(id = "a", name = "March")
        val settings = AppSettings(dateStamp = false)
        val json = buildManifest(settings, listOf(film), 1L)
        assertEquals(listOf(film), parseManifestFilms(json))
        assertEquals(false, applyManifestSettings(AppSettings(), json).dateStamp)
    }

    @Test
    fun oldManifestWithRangeStillReads() {
        val old = """{"films":[{"id":"a","name":"March","rangeType":"MONTH","rangeFrom":"2025-03-01","rangeTo":"2025-03-31"}]}"""
        val film = parseManifestFilms(old).single()
        assertEquals(FilmRangeType.MONTH, film.rangeType)
        assertEquals(LocalDate(2025, 3, 31), film.rangeTo)
        assertFalse(buildManifest(AppSettings(), listOf(film), 1L).contains("rangeFrom"))
    }

    @Test
    fun badManifestIsHarmless() {
        assertEquals(emptyList(), parseManifestFilms("not json"))
        assertEquals(AppSettings(), applyManifestSettings(AppSettings(), "{"))
    }

    @Test
    fun stages() {
        val scan = RestoreScan("f", listOf(parseRestoreItem(DriveFile("1", "a", mapOf("sliverDate" to "2025-01-01")))!!))
        assertEquals(RestoreStage.SCANNING, restoreStage(null, false, false, null, 0))
        assertEquals(RestoreStage.NONE, restoreStage(RestoreScan("f", emptyList()), false, false, null, 0))
        assertEquals(RestoreStage.FOUND, restoreStage(scan, false, false, null, 1))
        assertEquals(RestoreStage.RESTORING, restoreStage(scan, false, true, null, 1))
        assertEquals(RestoreStage.DONE, restoreStage(scan, false, true, null, 0))
        assertEquals("We found 1 clip", foundCopy(1))
        assertEquals("We found 3 clips", foundCopy(3))
    }

    @Test
    fun monthGrid() {
        val cells = monthCells(LocalDate(2024, 2, 10), listOf(LocalDate(2024, 2, 3)))
        assertEquals(3 + 29, cells.size)
        assertEquals(true, cells.filterNotNull().first { it.first == 3 }.second)
    }
}
