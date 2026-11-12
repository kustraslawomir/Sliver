package app.sliver.feature.gallery

import app.sliver.core.navigation.Route
import app.sliver.data.DaySource
import app.sliver.data.files.FileStore
import app.sliver.feature.camera.CaptureSession
import kotlinx.datetime.LocalDate

/**
 * Copies [uri] into tmp and fills [CaptureSession] for Edit clip or Photo clip. [asVideo] null = the item's natural kind
 * (a Motion Photo defaults to video); true / false force video or photo for a Motion Photo. Null when the item cannot be read.
 */
suspend fun loadIntoSession(provider: GalleryProvider, files: FileStore, uri: String, forDate: LocalDate?, asVideo: Boolean? = null): Route? {
    val motion = runCatching { provider.extractMotionVideo(uri) }.getOrNull()
    val wantPhoto = asVideo == false && motion != null
    if (wantPhoto) motion?.let { files.delete(it.path) }
    val video = !wantPhoto && (motion != null || provider.isVideo(uri))
    val path = (if (video) motion?.path else null) ?: runCatching { provider.copyToTmp(uri) }.getOrNull() ?: return null
    CaptureSession.path = path
    CaptureSession.forDate = forDate
    CaptureSession.sourceUri = uri
    CaptureSession.motionPhoto = motion != null
    CaptureSession.motion = video && motion != null
    CaptureSession.motionPresentationUs = if (video) motion?.presentationUs else null
    CaptureSession.source = if (video) DaySource.GALLERY else DaySource.PHOTO
    return if (video) Route.Trim else Route.PhotoTrim
}
