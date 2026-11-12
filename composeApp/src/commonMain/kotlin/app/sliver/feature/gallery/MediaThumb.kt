package app.sliver.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Small thumbnail of a gallery photo or video by content uri; null until loaded or when unreadable. */
@Composable
expect fun rememberMediaThumb(uri: String): ImageBitmap?
