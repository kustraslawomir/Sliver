package app.sliver.feature.gallery

import androidx.compose.ui.graphics.ImageBitmap

/** Read-only view of the device gallery. Originals are never modified; picks are copied to a tmp path. */
interface GalleryProvider {
    suspend fun load(): List<GalleryItem>
    suspend fun thumbnail(uri: String, sizePx: Int): ImageBitmap?

    /** Copies the picked media into the app's tmp area and returns the new path. */
    suspend fun copyToTmp(uri: String): String
}
