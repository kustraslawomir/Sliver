package app.sliver.feature.gallery

import androidx.compose.runtime.Composable
import app.sliver.core.log.logDebug

// TODO iOS later: PHPickerViewController.
@Composable
actual fun rememberMediaPicker(onPicked: (String) -> Unit): () -> Unit = { logDebug("MediaPicker", "not on iOS yet") }

@Composable
actual fun rememberMultiMediaPicker(onPicked: (List<String>) -> Unit): () -> Unit = { logDebug("MediaPicker", "not on iOS yet") }
