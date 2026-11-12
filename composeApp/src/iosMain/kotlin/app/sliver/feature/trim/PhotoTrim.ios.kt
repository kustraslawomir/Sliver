package app.sliver.feature.trim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.sliver.data.files.readFileBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

@Composable
actual fun rememberPhoto(path: String): ImageBitmap? {
    val photo by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            readFileBytes(path)?.let { runCatching { Image.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull() }
        }
    }
    return photo
}
