package app.sliver.feature.looks

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.LookParams

/** Cheap CPU approximation of a look on a still frame (LUT, contrast, saturation, fade, vignette) for thumbnails and photo preview. */
fun interface LookRenderer {
    suspend fun render(frame: ImageBitmap, params: LookParams, maxEdge: Int): ImageBitmap
}

@Composable
expect fun rememberLookRenderer(): LookRenderer
