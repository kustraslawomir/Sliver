package app.sliver.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.Lens

/** Exposure compensation indices [min]..[max]; one index is [step] EV. */
data class ExposureRange(val min: Int, val max: Int, val step: Float)

interface CameraEngine {
    /** Latest OrientationEventListener degrees; applied as target rotation before each capture. */
    var deviceDegrees: Int
    /** Records to [path]; [onResult] gets the path on success. */
    fun startRecording(path: String, onResult: (Result<String>) -> Unit)
    fun stopRecording()
    /**
     * AF+AE+AWB at a PreviewView point in px; [lock] keeps it until the next call, otherwise it auto-cancels after 3 s.
     * [onResult] gets true when the camera settled, false when it could not or the request was cancelled.
     */
    fun focus(x: Float, y: Float, lock: Boolean, onResult: (Boolean) -> Unit)
    /** Exposure compensation range of the bound camera, or null when unsupported. */
    fun exposureRange(): ExposureRange?
    fun setExposureIndex(index: Int)
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

/** Hand-off from capture to Trim / PhotoTrim until those screens take a route argument. */
object CaptureSession {
    var path: String? = null
    var forDate: kotlinx.datetime.LocalDate? = null
    var source: app.sliver.data.DaySource = app.sliver.data.DaySource.CAMERA
    var motionPresentationUs: Long? = null
    var motion: Boolean = false
}
