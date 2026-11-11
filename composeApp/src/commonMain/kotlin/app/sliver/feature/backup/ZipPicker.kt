package app.sliver.feature.backup

import androidx.compose.runtime.Composable

/** Returns a launcher that asks the user where to save a .zip (SAF); [onPicked] gets the document uri. */
@Composable
expect fun rememberZipSavePicker(onPicked: (String) -> Unit): (suggestedName: String) -> Unit
