package app.sliver.feature.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable

private val ImageAndVideo = PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)

@Composable
actual fun rememberMediaPicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> if (uri != null) onPicked(uri.toString()) }
    return { launcher.launch(ImageAndVideo) }
}

@Composable
actual fun rememberMultiMediaPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) onPicked(uris.map { it.toString() })
    }
    return { launcher.launch(ImageAndVideo) }
}
