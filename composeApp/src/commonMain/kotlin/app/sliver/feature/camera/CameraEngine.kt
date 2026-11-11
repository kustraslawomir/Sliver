package app.sliver.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.Lens

interface CameraEngine {
    /** Records to [path]; [onResult] gets the path on success. */
    fun startRecording(path: String, onResult: (Result<String>) -> Unit)
    fun stopRecording()
    fun takePhoto(path: String, onResult: (Result<String>) -> Unit)
}

@Composable
expect fun rememberCameraEngine(): CameraEngine

/** Binds the live preview and use cases for the given config; [onReady] fires once frames can be captured. */
@Composable
expect fun CameraPreview(
    engine: CameraEngine,
    mode: CameraMode,
    lens: Lens,
    torch: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
)

/** Thumbnail of the newest gallery item, or null when unavailable. */
@Composable
expect fun rememberLatestGalleryThumb(): ImageBitmap?

/** Hand-off from capture to Trim / PhotoTrim until those screens take a route argument. */
object CaptureSession {
    var path: String? = null
}
