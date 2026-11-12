package app.sliver.feature.backup

import androidx.compose.runtime.Composable
import app.sliver.core.log.logDebug

// TODO iOS later: UIDocumentPickerViewController export.
@Composable
actual fun rememberZipSavePicker(onPicked: (String) -> Unit): (suggestedName: String) -> Unit =
    { logDebug("ZipPicker", "not on iOS yet") }
