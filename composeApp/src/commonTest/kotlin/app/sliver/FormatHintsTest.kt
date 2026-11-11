package app.sliver

import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.db.Film
import app.sliver.feature.camera.activeFilm
import app.sliver.feature.camera.cameraChipLabel
import app.sliver.feature.camera.cinemaBandFraction
import app.sliver.feature.camera.cropYFromTop
import app.sliver.feature.camera.iconRotation
import app.sliver.feature.camera.isUpright
import app.sliver.feature.camera.topFromCropY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatHintsTest {
    private fun film(id: String, live: Boolean, format: FilmFormat = FilmFormat.CINEMA) =
        Film(id, id, format, FilmRangeType.YEAR, live = live)

    @Test
    fun activeFilmFallsBackToFirstLive() {
        val films = listOf(film("a", false), film("b", true), film("c", true))
        assertEquals("c", activeFilm(films, "c")?.id)
        assertEquals("b", activeFilm(films, "a")?.id)
        assertNull(activeFilm(listOf(film("a", false)), null))
    }

    @Test
    fun chipLabelShowsFilmAndFormat() {
        assertEquals("FOR: b · Story 9:16", cameraChipLabel(film("b", true, FilmFormat.STORY), FilmFormat.CINEMA))
        assertEquals("FOR: New film · Cinema 16:9", cameraChipLabel(null, FilmFormat.CINEMA))
    }

    @Test
    fun uprightAndIconRotation() {
        assertTrue(isUpright(0))
        assertTrue(isUpright(350))
        assertTrue(isUpright(180))
        assertFalse(isUpright(90))
        assertFalse(isUpright(270))
        assertEquals(-90f, iconRotation(80))
        assertEquals(0f, iconRotation(355))
    }

    @Test
    fun bandAndCropYRoundTrip() {
        val band = cinemaBandFraction(900f, 1600f)
        assertEquals(0.3164f, band, 0.001f)
        assertEquals(0.5f, cropYFromTop(topFromCropY(0.5f, band), band), 0.0001f)
        assertEquals(0f, cropYFromTop(0f, band))
        assertEquals(1f, cropYFromTop(1f - band, band), 0.0001f)
    }
}
