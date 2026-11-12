package app.sliver.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.Lens

private val notYet = UnsupportedOperationException("Camera not on iOS yet")

// TODO iOS later: AVCaptureSession engine. Until then capture fails cleanly and the preview stays black.
@Composable
actual fun rememberCameraEngine(): CameraEngine = remember {
    object : CameraEngine {
        override var deviceDegrees: Int = 0
        override fun startRecording(path: String, onResult: (Result<String>) -> Unit) = onResult(Result.failure(notYet))
        override fun stopRecording() = Unit
        override fun focus(x: Float, y: Float, lock: Boolean, onResult: (Boolean) -> Unit) = onResult(false)
        override fun exposureRange(): ExposureRange? = null
        override fun setExposureIndex(index: Int) = Unit
        override fun zoomRange(): ClosedFloatingPointRange<Float>? = null
        override fun setZoom(ratio: Float) = Unit
        override fun takePhoto(path: String, onResult: (Result<String>) -> Unit) = onResult(Result.failure(notYet))
    }
}

@Composable
actual fun CameraPreview(
    engine: CameraEngine,
    mode: CameraMode,
    lens: Lens,
    torch: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
) = Box(modifier.background(Color.Black))
