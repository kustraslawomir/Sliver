package app.sliver

import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import app.sliver.feature.films.audioPlan
import app.sliver.feature.films.dateStamp
import app.sliver.feature.films.dipFactor
import app.sliver.feature.films.fitScale
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilmRenderLogicTest {
    @Test
    fun cutNeverDips() {
        assertEquals(1f, dipFactor(FilmCut.CUT, 0))
    }

    @Test
    fun fadeDipsAtBoundariesOnly() {
        assertEquals(0f, dipFactor(FilmCut.FADE, 0))
        assertEquals(1f, dipFactor(FilmCut.FADE, 500))
        assertEquals(0.5f, dipFactor(FilmCut.FADE, 60), 0.001f)
        assertEquals(0.5f, dipFactor(FilmCut.FADE, 940), 0.001f)
    }

    @Test
    fun audioModes() {
        val original = audioPlan(FilmAudio.ORIGINAL, "asset:music/drift.aac")
        assertTrue(original.keepClipAudio && !original.music)
        val music = audioPlan(FilmAudio.MUSIC, "asset:music/drift.aac")
        assertFalse(music.keepClipAudio)
        assertTrue(music.music)
        val mix = audioPlan(FilmAudio.MIX, "asset:music/drift.aac")
        assertTrue(mix.keepClipAudio && mix.music)
        assertEquals(0.3f, mix.musicVolume)
        assertTrue(audioPlan(FilmAudio.MUSIC, null).keepClipAudio)
    }

    @Test
    fun fitScaleLetterboxesPortraitIntoCinema() {
        val (sx, sy) = fitScale(9f / 16f, 16f / 9f)
        assertEquals(1f, sy)
        assertEquals((9f / 16f) / (16f / 9f), sx, 0.001f)
    }

    @Test
    fun dateStampFormat() {
        assertEquals("5 Mar 2025", dateStamp(LocalDate(2025, 3, 5)))
    }
}
