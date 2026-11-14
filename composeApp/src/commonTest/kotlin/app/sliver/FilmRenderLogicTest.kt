package app.sliver

import app.sliver.data.FilmAudio
import app.sliver.feature.films.audioPlan
import app.sliver.feature.films.fitScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilmRenderLogicTest {
    @Test
    fun audioModes() {
        val original = audioPlan(FilmAudio.ORIGINAL, "/data/music/1_a.mp3")
        assertTrue(original.keepClipAudio && !original.music)
        val music = audioPlan(FilmAudio.MUSIC, "/data/music/1_a.mp3")
        assertFalse(music.keepClipAudio)
        assertTrue(music.music)
        val mix = audioPlan(FilmAudio.MIX, "/data/music/1_a.mp3")
        assertTrue(mix.keepClipAudio && mix.music)
        assertEquals(0.3f, mix.musicVolume)
        assertTrue(audioPlan(FilmAudio.MUSIC, null).keepClipAudio)
        assertTrue(audioPlan(FilmAudio.MUSIC, "asset:music/drift.aac").let { it.keepClipAudio && !it.music })
    }

    @Test
    fun fitScaleLetterboxesPortraitIntoCinema() {
        val (sx, sy) = fitScale(9f / 16f, 16f / 9f)
        assertEquals(1f, sy)
        assertEquals((9f / 16f) / (16f / 9f), sx, 0.001f)
    }

}
