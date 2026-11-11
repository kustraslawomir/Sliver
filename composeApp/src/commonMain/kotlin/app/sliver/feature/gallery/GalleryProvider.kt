package app.sliver.feature.gallery

/** Reads media the user handed over through the system Photo Picker. Originals are never modified. */
interface GalleryProvider {
    suspend fun isVideo(uri: String): Boolean

    /** Picked items with their taken date; items whose date cannot be read are left out. */
    suspend fun describe(uris: List<String>): List<GalleryItem>

    /** Copies the picked media into the app's tmp area and returns the new path. */
    suspend fun copyToTmp(uri: String): String
}
