package app.sliver.feature.gallery

/** Reads media the user handed over through the system Photo Picker. Originals are never modified. */
interface GalleryProvider {
    suspend fun isVideo(uri: String): Boolean

    /** Picked items with their taken date; items whose date cannot be read are left out. */
    suspend fun describe(uris: List<String>): List<GalleryItem>

    /** Copies the picked media into the app's tmp area and returns the new path. */
    suspend fun copyToTmp(uri: String): String

    /** Like [copyToTmp] but into storage that is not swept, so a batch survives process death; the caller deletes it. */
    suspend fun keep(uri: String): String

    /** If the picked photo is a Motion Photo, writes its embedded video to tmp; null otherwise. */
    suspend fun extractMotionVideo(uri: String): MotionClip?
}

class MotionClip(val path: String, val presentationUs: Long?)
