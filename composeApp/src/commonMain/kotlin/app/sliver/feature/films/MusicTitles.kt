package app.sliver.feature.films

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf

/**
 * Audit v1.8 §5: a readable title from a file name — no extension, no import prefix ("<millis>_"), "_" and "-" as spaces,
 * no copy markers ("(1)", "_1_") or trailing ids, title case. "sub_clair-suspense-628719 _1_.mp3" → "Sub Clair Suspense".
 */
fun cleanTrackTitle(fileName: String): String {
    var t = fileName.substringAfterLast('/').replace(Regex("^\\d{10,}_"), "")
    t = t.substringBeforeLast('.', t)
    t = t.replace(Regex("\\(\\d+\\)|_\\d+_\\s*$"), " ")
    t = t.replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ").trim()
    val words = t.split(' ').toMutableList()
    // Trailing ids (3+ digits, e.g. "628719") go; short numbers ("Track 2") are part of the name.
    while (words.size > 1 && words.last().count { it.isDigit() } >= 3 && words.last().count { it.isDigit() } * 2 >= words.last().length) words.removeAt(words.lastIndex)
    t = words.joinToString(" ")
    if (t.isBlank()) return "Audio"
    return t.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
}

/** Titles the owner gave tracks (path → title); loaded by [rememberMusicStore]. */
object MusicTitles {
    val overrides = mutableStateMapOf<String, String>()
}

fun trackTitle(path: String): String = MusicTitles.overrides[path] ?: cleanTrackTitle(path)

/** "2:31 · fills the whole film" / "1:58 · loops 2×" against the film's length. */
fun fitCaption(durationMs: Long, filmMs: Long): String {
    if (durationMs <= 0) return "–"
    val len = audioTime(durationMs)
    if (durationMs >= filmMs || filmMs <= 0) return "$len · fills the whole film"
    val loops = ((filmMs + durationMs - 1) / durationMs).toInt()
    return "$len · loops $loops×"
}

/** BuilderV18 Sound row: value ("Original", "Music", "Original + 1 track", "Muted") and sub line ("Clair Suspense · 70 %"). */
fun soundSummary(mix: AudioMix): Pair<String, String?> {
    val on = mix.layers.filter { !it.muted }
    val clips = mix.clipVolume > 0
    val value = when {
        on.isEmpty() && clips -> "Original"
        on.isEmpty() -> "Muted"
        !clips -> if (on.size == 1) "Music" else "${on.size} tracks"
        else -> "Original + ${on.size} ${if (on.size == 1) "track" else "tracks"}"
    }
    val sub = on.firstOrNull()?.let { first ->
        trackTitle(first.path) + if (on.size > 1) " + ${on.size - 1} more" else " · ${first.volume} %"
    }
    return value to sub
}

/** Plays one track at a time from its start, for the music list. */
interface TrackPreview {
    val playing: String?
    fun toggle(path: String)
    fun stop()
}

@Composable
expect fun rememberTrackPreview(): TrackPreview

/** The app's music folder: owner titles and removal. */
interface MusicStore {
    fun rename(path: String, title: String)
    fun remove(path: String)
}

@Composable
expect fun rememberMusicStore(): MusicStore
