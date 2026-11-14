package app.sliver

import app.sliver.data.db.Film
import app.sliver.feature.films.AudioLayer
import app.sliver.feature.films.AudioMix
import app.sliver.feature.films.cleanTrackTitle
import app.sliver.feature.films.coverIndex
import app.sliver.feature.films.filmVersions
import app.sliver.feature.films.fitCaption
import app.sliver.feature.films.posterCaption
import app.sliver.feature.films.soundSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MusicTitlesTest {
    @Test
    fun titlesAreCleaned() {
        assertEquals("Sub Clair Suspense", cleanTrackTitle("/data/music/1763000000000_sub_clair-suspense-628719 _1_.mp3"))
        assertEquals("Dziurawy Obiad", cleanTrackTitle("dziurawy_obiad (1).m4a"))
        assertEquals("Beyond The Veil Of Light", cleanTrackTitle("beyond-the-veil-of-light.mp3"))
        assertEquals("Track 2", cleanTrackTitle("track 2.wav"))
    }

    @Test
    fun captionsSayHowTheTrackFits() {
        assertEquals("2:31 · fills the whole film", fitCaption(151_000, 120_000))
        assertEquals("1:58 · loops 2×", fitCaption(118_000, 155_000))
    }

    @Test
    fun soundRowSummary() {
        assertEquals("Original" to null, soundSummary(AudioMix()))
        val one = AudioMix(layers = listOf(AudioLayer("/m/1763000000000_clair_suspense.mp3", 1000, volume = 70)))
        assertEquals("Original + 1 track" to "Clair Suspense · 70 %", soundSummary(one))
        assertEquals("Music", soundSummary(one.copy(clipVolume = 0)).first)
    }

    @Test
    fun sameNamedFilmsAreVersionsWithTheirOwnCover() {
        val a = Film("a", "Lena", updatedAt = 1)
        val b = Film("b", "Lena", updatedAt = 2)
        val c = Film("c", "October", updatedAt = 3)
        assertEquals(mapOf("a" to 1, "b" to 2), filmVersions(listOf(b, c, a)))
        assertNotEquals(coverIndex(155, 1), coverIndex(155, 2))
        assertEquals("2:35 · v3 · Kiosk", posterCaption(155, 3, "Kiosk"))
        assertEquals("0:23 · Natural", posterCaption(23, null, null))
    }
}
