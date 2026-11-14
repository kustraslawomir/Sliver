package app.sliver.feature.autoimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.runtime.Composable

@Composable
actual fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher =
        rememberLauncherForActivityResult(OpenDocumentTree()) { uri -> if (uri != null) onPicked(uri.toString()) }
    return { launcher.launch(null) }
}
