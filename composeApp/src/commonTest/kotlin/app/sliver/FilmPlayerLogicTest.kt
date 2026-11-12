package app.sliver

import app.sliver.feature.films.dayIndexAtMs
import app.sliver.feature.films.filmInfoLine
import app.sliver.feature.films.slitFraction
import app.sliver.feature.films.titleAlpha
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilmPlayerLogicTest {
    @Test
    fun slitEasesOutCubicToFullFrameAtOneSecond() {
        assertEquals(0f, slitFraction(0))
        assertEquals(1f, slitFraction(1000))
        assertEquals(1f, slitFraction(5000))
        assertEquals(0.875f, slitFraction(500), 0.0001f)
        assertTrue(slitFraction(250) > 0.25f)
    }

    @Test
    fun titleFadesInFrom1400ms() {
        assertEquals(0f, titleAlpha(1400))
        assertEquals(0.5f, titleAlpha(1700), 0.0001f)
        assertEquals(1f, titleAlpha(2000))
    }

    @Test
    fun dayIndexFollowsPositionAndClampsOnTitleCard() {
        assertEquals(0, dayIndexAtMs(999, 3))
        assertEquals(1, dayIndexAtMs(1000, 3))
        assertEquals(2, dayIndexAtMs(9000, 3))
        assertEquals(0, dayIndexAtMs(500, 0))
    }

    @Test
    fun infoLineRoundsMegabytes() {
        assertEquals("MP4 · 1080p · 30 fps · 9 MB · kept in Fragments", filmInfoLine(9_400_000))
        assertEquals("MP4 · 1080p · 30 fps · 1 MB · kept in Fragments", filmInfoLine(0))
    }
}
