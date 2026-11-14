package app.sliver.feature.films

import app.sliver.data.FilmAudio
import app.sliver.data.db.Film
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Owner 14 Nov ("like DaVinci"): one audio file placed on the film. It plays from [startMs] of the film, using the part
 * [inMs]..[outMs] of the file (null = to its end), at [volume] 0..100, once or repeated to the end of the film.
 */
@Serializable
data class AudioLayer(
    val path: String,
    val durationMs: Long,
    val startMs: Long = 0,
    val inMs: Long = 0,
    val outMs: Long? = null,
    val volume: Int = 100,
    val loop: Boolean = true,
    val muted: Boolean = false,
)

/** The film's sound: its clips' own sound at [clipVolume] (except [mutedClips], by date) plus any audio layers. */
@Serializable
data class AudioMix(
    val clipVolume: Int = 100,
    val mutedClips: Set<String> = emptySet(),
    val layers: List<AudioLayer> = emptyList(),
)

private val mixJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

fun AudioMix.encode(): String = mixJson.encodeToString(this)

fun decodeAudioMix(text: String?): AudioMix? = text?.let { runCatching { mixJson.decodeFromString<AudioMix>(it) }.getOrNull() }

/** Films saved before layers: Music = clips silent + the track at full volume, Mix = clips + the track at 30 %. */
fun Film.audioMix(durationOf: (String) -> Long = { 0L }): AudioMix {
    decodeAudioMix(audioMix)?.let { return it }
    val plan = audioPlan(audio, track, musicVolume)
    val layer = usableTrack(track)?.takeIf { plan.music }?.let {
        AudioLayer(it, durationOf(it), volume = (plan.musicVolume * 100).toInt(), loop = true)
    }
    return AudioMix(clipVolume = if (plan.keepClipAudio) 100 else 0, layers = listOfNotNull(layer))
}

/** Old builds only read audio/track/musicVolume; keep them close to the mix so a synced film still sounds right there. */
fun AudioMix.legacyAudio(): Triple<FilmAudio, String?, Int?> {
    val first = layers.firstOrNull { !it.muted } ?: return Triple(FilmAudio.ORIGINAL, null, null)
    return Triple(if (clipVolume == 0) FilmAudio.MUSIC else FilmAudio.MIX, first.path, first.volume)
}

/** Length of the used part of the file. */
val AudioLayer.lengthMs: Long get() = ((outMs ?: durationMs).coerceAtMost(durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE) - inMs).coerceAtLeast(0)

/** Film time where the layer stops: the film's end when it repeats, otherwise after one pass. */
fun AudioLayer.endMs(filmMs: Long): Long = if (loop) filmMs else (startMs + lengthMs).coerceAtMost(filmMs)

/** Position in the file at film time [filmMs], or null when the layer is silent there. */
fun layerPositionAt(layer: AudioLayer, filmMs: Long): Long? {
    val t = filmMs - layer.startMs
    val len = layer.lengthMs
    if (t < 0 || len <= 0) return null
    if (!layer.loop && t >= len) return null
    return layer.inMs + t % len
}

/** Parts of the file (in..out ms) placed back to back from [AudioLayer.startMs] to fill at most the film's [filmMs]. */
fun layerSegments(layer: AudioLayer, filmMs: Long): List<LongRange> {
    val room = filmMs - layer.startMs
    val len = layer.lengthMs
    if (room <= 0 || len <= 0 || layer.muted) return emptyList()
    val out = mutableListOf<LongRange>()
    var left = room
    while (left > 0) {
        val take = minOf(len, left)
        out += layer.inMs..(layer.inMs + take)
        left -= take
        if (!layer.loop) break
    }
    return out
}

/** Gain for a clip's own sound in this film (0 = silent). */
fun AudioMix.clipGain(date: String, keepAudio: Boolean): Float =
    if (!keepAudio || date in mutedClips) 0f else clipVolume.coerceIn(0, 100) / 100f

/** Rendered film length: one second per clip plus the closing card. */
fun filmLengthMs(clips: Int): Long = clips * FILM_FRAME_MS + FILM_TITLE_MS

/** "1:05" for audio lengths and positions. */
fun audioTime(ms: Long): String {
    val s = (ms.coerceAtLeast(0) + 500) / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
