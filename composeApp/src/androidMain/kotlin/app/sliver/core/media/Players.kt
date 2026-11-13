package app.sliver.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

/**
 * Every in-app player. ExoPlayer's default load control may hold ~130 MB of video samples in Java byte arrays; with
 * 4K/8K HEVC from the gallery two players (preview + scrub) filled the 256 MB heap and the app died with
 * OutOfMemoryError (Samsung logcat 2026-11-13). Clips here are seconds long, so a few seconds and 16 MB is plenty.
 */
@OptIn(UnstableApi::class)
fun newPlayer(context: Context): ExoPlayer = ExoPlayer.Builder(context)
    .setLoadControl(
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_500, 4_000, 250, 500)
            .setTargetBufferBytes(PLAYER_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build(),
    )
    .build()

const val PLAYER_BUFFER_BYTES = 16 * 1024 * 1024
