package app.sliver.feature.gallery

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMediaPicker(onPicked: (String) -> Unit): () -> Unit = TODO("iOS later")

@Composable
actual fun rememberMultiMediaPicker(onPicked: (List<String>) -> Unit): () -> Unit = TODO("iOS later")
