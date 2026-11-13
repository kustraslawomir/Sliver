package app.sliver.feature.gallery

import androidx.compose.runtime.Composable

/** Launcher for the system Photo Picker with multi-select (not called on cancel). */
@Composable
expect fun rememberMultiMediaPicker(onPicked: (List<String>) -> Unit): () -> Unit
