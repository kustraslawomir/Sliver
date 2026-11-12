package app.sliver.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    /** Zoom ratios reachable on the current lens, below 1× when there is an ultra-wide; null until bound. */
    fun zoomRange(): ClosedFloatingPointRange<Float>?
    /** Zoom ratio relative to the main lens (0.6 = ultra-wide); kept across rebinds of the same lens. */
    fun setZoom(ratio: Float)
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
        set(v) {
            field = v
            sourceUri = null
            motionPhoto = false
            draft = null
        }
    var forDate: kotlinx.datetime.LocalDate? = null
    var source: app.sliver.data.DaySource = app.sliver.data.DaySource.CAMERA
    var motionPresentationUs: Long? = null
    var motion: Boolean = false

    /** Gallery uri the current path was copied from, and whether it is a Motion Photo (so Video / Photo can be switched). */
    var sourceUri: String? = null
    var motionPhoto: Boolean = false

    /** Set while a draft is being reviewed; the editor then shows the draft header and "Keep this clip". */
    var draft: app.sliver.feature.autoimport.DraftContext? = null

    /** Bumped to rebuild the editor in place after a swap or a move to another draft. */
    var revision by androidx.compose.runtime.mutableIntStateOf(0)
}
