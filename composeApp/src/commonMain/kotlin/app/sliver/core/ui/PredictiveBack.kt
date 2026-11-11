package app.sliver.core.ui

import androidx.compose.runtime.Composable

/** Back gesture with progress 0..1 while swiping; [onBack] fires when committed, progress returns to 0 on cancel. */
@Composable
expect fun PredictiveBackHandler(enabled: Boolean = true, onProgress: (Float) -> Unit, onBack: () -> Unit)
