package app.sliver.feature.camera

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
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

class AndroidCameraEngine(private val context: Context) : CameraEngine {
    private val executor = ContextCompat.getMainExecutor(context)
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null
    override var deviceDegrees: Int = 0

    fun bind(
        provider: ProcessCameraProvider,
        owner: androidx.lifecycle.LifecycleOwner,
        view: PreviewView,
        mode: CameraMode,
        lens: Lens,
        torch: Boolean,
    ) {
        val selector = if (lens == Lens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        provider.unbindAll()
        camera = if (mode == CameraMode.VIDEO) {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)))
                .build()
            val vc = VideoCapture.withOutput(recorder).also { videoCapture = it }
            provider.bindToLifecycle(owner, selector, preview, vc)
        } else {
            val ic = ImageCapture.Builder()
                .setFlashMode(if (torch) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build().also { imageCapture = it }
            provider.bindToLifecycle(owner, selector, preview, ic)
        }
        if (mode == CameraMode.VIDEO) camera?.cameraControl?.enableTorch(torch)
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
    val view = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
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

@Composable
actual fun rememberLatestGalleryThumb(): ImageBitmap? {
    val context = LocalContext.current
    var thumb by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        thumb = withContext(Dispatchers.IO) { runCatching { latestThumb(context) }.getOrNull() }
    }
    return thumb
}

private fun latestThumb(context: Context): ImageBitmap? {
    val args = Bundle().apply {
        putString(
            android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE},${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})",
        )
        putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.DATE_ADDED))
        putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 1)
    }
    val collection = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE)
    context.contentResolver.query(collection, projection, args, null)?.use { c ->
        if (!c.moveToFirst()) return null
        val base = if (c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = ContentUris.withAppendedId(base, c.getLong(0))
        return context.contentResolver.loadThumbnail(uri, Size(192, 192), null).asImageBitmap()
    }
    return null
}
