package app.sliver.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberZipSavePicker(onPicked: (String) -> Unit): (suggestedName: String) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onPicked(uri.toString())
    }
    return { name -> launcher.launch(name) }
}
