package app.sliver.feature.films

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import java.io.File
import kotlinx.serialization.json.Json

@Composable
actual fun rememberTrackPreview(): TrackPreview {
    val context = LocalContext.current
    val preview = remember {
        object : TrackPreview {
            var current by mutableStateOf<String?>(null)
            val player = app.sliver.core.media.newPlayer(context).apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) current = null }
                })
            }
            override val playing: String? get() = current
            override fun toggle(path: String) {
                if (current == path) { stop(); return }
                player.setMediaItem(MediaItem.fromUri(trackUri(path)))
                player.prepare()
                player.play()
                current = path
            }
            override fun stop() { player.stop(); current = null }
        }
    }
    DisposableEffect(Unit) { onDispose { preview.player.release() } }
    return preview
}

private val titlesJson = Json { ignoreUnknownKeys = true }

private fun titlesFile(context: Context) = File(File(context.filesDir, "music").apply { mkdirs() }, ".titles.json")

@Composable
actual fun rememberMusicStore(): MusicStore {
    val context = LocalContext.current
    return remember {
        runCatching { titlesJson.decodeFromString<Map<String, String>>(titlesFile(context).readText()) }.getOrNull()
            ?.let { saved -> MusicTitles.overrides.putAll(saved) }
        object : MusicStore {
            private fun save() = runCatching { titlesFile(context).writeText(titlesJson.encodeToString(MusicTitles.overrides.toMap())) }
            override fun rename(path: String, title: String) {
                if (title.isBlank()) MusicTitles.overrides.remove(path) else MusicTitles.overrides[path] = title.trim()
                save()
            }
            override fun remove(path: String) {
                File(path).takeIf { it.parentFile?.name == "music" }?.delete()
                MusicTitles.overrides.remove(path)
                save()
            }
        }
    }
}
