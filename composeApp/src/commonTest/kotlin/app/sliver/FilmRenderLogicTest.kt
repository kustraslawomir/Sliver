package app.sliver

import app.sliver.data.FilmAudio
import app.sliver.feature.films.audioPlan
import app.sliver.feature.films.fitScale
import app.sliver.feature.films.musicDrift
import app.sliver.feature.films.musicPositionAt
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

    @Test
    fun musicFollowsTheFilm() {
        assertEquals(0L, musicPositionAt(0, 0, 30_000))
        assertEquals(12_400L, musicPositionAt(12, 400, 30_000))
        assertEquals(5_000L, musicPositionAt(35, 0, 30_000))
        assertEquals(100L, musicDrift(29_950, 50, 30_000))
        assertEquals(2_000L, musicDrift(1_000, 3_000, 30_000))
    }
}
