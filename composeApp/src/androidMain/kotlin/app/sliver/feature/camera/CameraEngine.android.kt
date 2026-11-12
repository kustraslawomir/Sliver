package app.sliver.feature.camera

import app.sliver.core.log.logDebug
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.DynamicRange
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.Lens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.hypot

private const val TAG = "SliverFocus"
private const val UHD_BITRATE = 48_000_000
private const val FHD_BITRATE = 20_000_000
private const val FULL_FRAME_DIAGONAL_MM = 43.27f

class AndroidCameraEngine(private val context: Context) : CameraEngine {
    private val executor = ContextCompat.getMainExecutor(context)
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null
    override var deviceDegrees: Int = 0
    private var previewView: PreviewView? = null

    override fun focus(x: Float, y: Float, lock: Boolean, onResult: (Boolean) -> Unit) {
        val view = previewView
        val cam = camera
        if (view == null || cam == null) {
            logDebug(TAG, "focus: skipped, preview=${view != null} camera=${cam != null}")
            return onResult(false)
        }
        val point = view.meteringPointFactory.createPoint(x, y)
        val flags = FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        val action = FocusMeteringAction.Builder(point, flags).apply {
            if (lock) disableAutoCancel() else setAutoCancelDuration(FOCUS_AUTO_CANCEL_S, TimeUnit.SECONDS)
        }.build()
        val supported = cam.cameraInfo.isFocusMeteringSupported(action)
        logDebug(TAG, "focus: point=($x,$y) view=${view.width}x${view.height} lock=$lock supported=$supported")
        if (!supported) return onResult(false)
        logDebug(TAG, "startFocusAndMetering called lock=$lock")
        val future = cam.cameraControl.startFocusAndMetering(action)
        future.addListener({
            val ok = runCatching { future.get().isFocusSuccessful }.onFailure { logDebug(TAG, "focus future failed: $it") }.getOrDefault(false)
            logDebug(TAG, "startFocusAndMetering result: focusSuccessful=$ok lock=$lock")
            onResult(ok)
        }, executor)
    }

    override fun exposureRange(): ExposureRange? {
        val state = camera?.cameraInfo?.exposureState ?: return null
        if (!state.isExposureCompensationSupported) return null
        val r = state.exposureCompensationRange
        if (r.lower == 0 && r.upper == 0) return null
        return ExposureRange(r.lower, r.upper, state.exposureCompensationStep.toFloat())
    }

    override fun setExposureIndex(index: Int) {
        val control = camera?.cameraControl ?: return
        val future = control.setExposureCompensationIndex(index)
        logDebug(TAG, "setExposureCompensationIndex($index) requested")
        future.addListener({
            val applied = runCatching { future.get() }.getOrNull()
            logDebug(TAG, "setExposureCompensationIndex applied=$applied current=${camera?.cameraInfo?.exposureState?.exposureCompensationIndex}")
        }, executor)
    }

    private class Binding(
        val provider: ProcessCameraProvider,
        val owner: androidx.lifecycle.LifecycleOwner,
        val view: PreviewView,
        val mode: CameraMode,
        val lens: Lens,
        val torch: Boolean,
    )

    /** A separate ultra-wide camera, used when the main camera can't zoom below 1× itself. */
    private class UltraWide(val id: String, val ratio: Float)

    private var binding: Binding? = null
    private var mainInfo: CameraInfo? = null
    private var ultraWide: UltraWide? = null
    private var onUltraWide = false
    private var zoom = 1f

    override fun zoomRange(): ClosedFloatingPointRange<Float>? {
        val state = mainInfo?.zoomState?.value ?: return null
        return (ultraWide?.ratio ?: state.minZoomRatio)..state.maxZoomRatio
    }

    override fun setZoom(ratio: Float) {
        zoom = ratio
        val b = binding
        if (b != null && ultraWide != null && (ratio < 1f) != onUltraWide && recording == null) return bindNow(b)
        applyZoom()
    }

    private fun applyZoom() {
        val cam = camera ?: return
        val state = cam.cameraInfo.zoomState.value ?: return
        val ratio = if (onUltraWide) zoom / (ultraWide?.ratio ?: 1f) else zoom
        cam.cameraControl.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    fun bind(
        provider: ProcessCameraProvider,
        owner: androidx.lifecycle.LifecycleOwner,
        view: PreviewView,
        mode: CameraMode,
        lens: Lens,
        torch: Boolean,
    ) {
        if (binding?.lens != lens) zoom = 1f
        bindNow(Binding(provider, owner, view, mode, lens, torch))
    }

    private fun bindNow(b: Binding) {
        binding = b
        previewView = b.view
        val provider = b.provider
        val mainSelector = if (b.lens == Lens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val main = provider.getCameraInfo(mainSelector).also { mainInfo = it }
        val mainMin = main.zoomState.value?.minZoomRatio ?: 1f
        ultraWide = if (b.lens == Lens.BACK && mainMin >= 1f) findUltraWide(provider, main) else null
        onUltraWide = ultraWide != null && zoom < 1f
        val uwId = ultraWide?.id?.takeIf { onUltraWide }
        val selector = if (uwId == null) mainSelector else CameraSelector.Builder().addCameraFilter { infos -> infos.filter { cameraId(it) == uwId } }.build()
        val info = if (uwId == null) main else provider.getCameraInfo(selector)

        val displayRotation = b.view.display?.rotation ?: Surface.ROTATION_0
        val preview = Preview.Builder().setTargetRotation(displayRotation)
            .setPreviewStabilizationEnabled(b.mode == CameraMode.VIDEO && Preview.getPreviewCapabilities(info).isStabilizationSupported)
            .build().also { it.surfaceProvider = b.view.surfaceProvider }
        provider.unbindAll()
        camera = if (b.mode == CameraMode.VIDEO) {
            val caps = Recorder.getVideoCapabilities(info)
            // DECISION: 4K on the back lens when offered; stored days are 1080p, so 4K only buys sharper downscales.
            val uhd = b.lens == Lens.BACK && Quality.UHD in caps.getSupportedQualities(DynamicRange.SDR)
            val top = if (uhd) Quality.UHD else Quality.FHD
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(top, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)))
                .setTargetVideoEncodingBitRate(if (uhd) UHD_BITRATE else FHD_BITRATE)
                .build()
            val vc = VideoCapture.Builder(recorder)
                // Preview stabilization already covers the recording and keeps the preview matching it.
                .setVideoStabilizationEnabled(caps.isStabilizationSupported && !Preview.getPreviewCapabilities(info).isStabilizationSupported)
                .build().also { videoCapture = it }
            provider.bindToLifecycle(b.owner, selector, preview, vc)
        } else {
            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(if (b.torch) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build().also { imageCapture = it }
            provider.bindToLifecycle(b.owner, selector, preview, ic)
        }
        if (b.mode == CameraMode.VIDEO) camera?.cameraControl?.enableTorch(b.torch)
        camera?.cameraControl?.setExposureCompensationIndex(0)
        applyZoom()
        logDebug(TAG, "bind: mode=${b.mode} lens=${b.lens} zoom=$zoom range=${zoomRange()} ultraWide=${ultraWide?.ratio} on=$onUltraWide exposureRange=${exposureRange()}")
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun findUltraWide(provider: ProcessCameraProvider, main: CameraInfo): UltraWide? {
        val mainFocal = equivalentFocal(main) ?: return null
        val mainId = cameraId(main)
        return provider.availableCameraInfos
            .filter { it.lensFacing == CameraSelector.LENS_FACING_BACK && cameraId(it) != mainId }
            .mapNotNull { info -> equivalentFocal(info)?.let { UltraWide(cameraId(info), it / mainFocal) } }
            .filter { it.ratio < 0.9f }
            .minByOrNull { it.ratio }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun cameraId(info: CameraInfo): String = Camera2CameraInfo.from(info).cameraId

    /** 35 mm equivalent focal length, so lenses on different sensors compare. */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun equivalentFocal(info: CameraInfo): Float? {
        val c = Camera2CameraInfo.from(info)
        val focal = c.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull() ?: return null
        val sensor = c.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
        return focal * FULL_FRAME_DIAGONAL_MM / hypot(sensor.width, sensor.height)
    }

    @SuppressLint("MissingPermission")
    override fun startRecording(path: String, onResult: (Result<String>) -> Unit) {
        val vc = videoCapture ?: return onResult(Result.failure(IllegalStateException("camera not ready")))
        vc.targetRotation = surfaceRotation(deviceDegrees)
        recording = vc.output.prepareRecording(context, FileOutputOptions.Builder(File(path)).build())
            .withAudioEnabled()
            .start(executor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                    onResult(
                        if (event.hasError()) Result.failure(event.cause ?: IllegalStateException("record error ${event.error}"))
                        else Result.success(path),
                    )
                }
            }
    }

    override fun stopRecording() {
        recording?.stop()
    }

    override fun takePhoto(path: String, onResult: (Result<String>) -> Unit) {
        val ic = imageCapture ?: return onResult(Result.failure(IllegalStateException("camera not ready")))
        ic.targetRotation = surfaceRotation(deviceDegrees)
        ic.takePicture(
            ImageCapture.OutputFileOptions.Builder(File(path)).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResult(Result.success(path))
                override fun onError(e: ImageCaptureException) = onResult(Result.failure(e))
            },
        )
    }
}

@Composable
actual fun rememberCameraEngine(): CameraEngine {
    val context = LocalContext.current
    return remember { AndroidCameraEngine(context) }
}

@Composable
actual fun CameraPreview(
    engine: CameraEngine,
    mode: CameraMode,
    lens: Lens,
    torch: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({ provider = future.get() }, ContextCompat.getMainExecutor(context))
    }
    LaunchedEffect(provider, mode, lens, torch) {
        val p = provider ?: return@LaunchedEffect
        (engine as AndroidCameraEngine).bind(p, owner, view, mode, lens, torch)
        onReady()
    }
    AndroidView({ view }, modifier)
}

