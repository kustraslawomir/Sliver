package app.sliver.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.sliver.data.settings.CameraMode
import app.sliver.data.settings.Lens

@Composable
actual fun rememberCameraEngine(): CameraEngine = TODO("iOS later")

@Composable
actual fun CameraPreview(
    engine: CameraEngine,
    mode: CameraMode,
    lens: Lens,
    torch: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
): Unit = TODO("iOS later")

@Composable
actual fun rememberLatestGalleryThumb(): ImageBitmap? = TODO("iOS later")
