package app.sliver.feature.autoimport

import androidx.compose.runtime.Composable

/** Launcher for the system folder picker (not called on cancel); the result is a tree uri with read access to grant. */
@Composable
expect fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit
