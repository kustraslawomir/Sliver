package app.sliver

import app.sliver.data.FilmAudio
import app.sliver.data.db.Film
import app.sliver.feature.films.AudioLayer
import app.sliver.feature.films.AudioMix
import app.sliver.feature.films.FilmOptions
import app.sliver.feature.films.audioMix
import app.sliver.feature.films.clipGain
import app.sliver.feature.films.decodeAudioMix
import app.sliver.feature.films.encode
import app.sliver.feature.films.layerPositionAt
import app.sliver.feature.films.layerSegments
import app.sliver.feature.films.toFilm
import app.sliver.data.FilmFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioMixTest {
    private val song = AudioLayer("/m/1_song.mp3", durationMs = 30_000, startMs = 5_000, inMs = 10_000, outMs = 20_000)

    @Test
    fun layerFollowsFilmTime() {
        assertNull(layerPositionAt(song, 4_000))
        assertEquals(10_000L, layerPositionAt(song, 5_000))
        assertEquals(19_000L, layerPositionAt(song, 14_000))
        assertEquals(11_000L, layerPositionAt(song, 16_000)) // repeats
        assertNull(layerPositionAt(song.copy(loop = false), 16_000))
    }

    @Test
    fun renderSegmentsFillTheFilm() {
        assertEquals(listOf(10_000L..20_000L, 10_000L..20_000L, 10_000L..13_000L), layerSegments(song, 28_000))
        assertEquals(listOf(10_000L..20_000L), layerSegments(song.copy(loop = false), 60_000))
        assertEquals(listOf(10_000L..12_000L), layerSegments(song.copy(loop = false), 7_000))
        assertEquals(emptyList(), layerSegments(song.copy(muted = true), 60_000))
        assertEquals(emptyList(), layerSegments(song.copy(startMs = 9_000), 9_000))
    }

    @Test
    fun clipSound() {
        val mix = AudioMix(clipVolume = 50, mutedClips = setOf("2026-11-13"))
        assertEquals(0.5f, mix.clipGain("2026-11-12", true))
        assertEquals(0f, mix.clipGain("2026-11-13", true))
        assertEquals(0f, mix.clipGain("2026-11-12", false))
    }

    @Test
    fun oldFilmsBecomeOneLayer() {
        val music = Film("f", "n", audio = FilmAudio.MUSIC, track = "/m/1_a.mp3").audioMix { 60_000 }
        assertEquals(0, music.clipVolume)
        assertEquals(listOf(AudioLayer("/m/1_a.mp3", 60_000, volume = 100)), music.layers)
        val mix = Film("f", "n", audio = FilmAudio.MIX, track = "/m/1_a.mp3", musicVolume = 40).audioMix()
        assertEquals(100, mix.clipVolume)
        assertEquals(40, mix.layers.single().volume)
        assertEquals(AudioMix(), Film("f", "n").audioMix())
    }

    @Test
    fun savedMixRoundTripsAndKeepsOldFieldsClose() {
        val mix = AudioMix(clipVolume = 0, mutedClips = setOf("2026-11-01"), layers = listOf(song, song.copy(path = "/m/2_b.mp3", volume = 30)))
        assertEquals(mix, decodeAudioMix(mix.encode()))
        val film = FilmOptions(mix = mix).toFilm("f", "n", FilmFormat.CINEMA)
        assertEquals(FilmAudio.MUSIC, film.audio)
        assertEquals("/m/1_song.mp3", film.track)
        assertEquals(mix, film.audioMix())
    }
}
