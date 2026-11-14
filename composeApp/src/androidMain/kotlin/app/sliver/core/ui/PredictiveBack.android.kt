package app.sliver.core.ui

import androidx.compose.runtime.Composable
import kotlin.coroutines.cancellation.CancellationException
import androidx.activity.compose.PredictiveBackHandler as ActivityPredictiveBackHandler

@Composable
actual fun PredictiveBackHandler(enabled: Boolean, onProgress: (Float) -> Unit, onBack: () -> Unit) {
    ActivityPredictiveBackHandler(enabled) { events ->
        try {
            events.collect { onProgress(it.progress) }
            onBack()
        } catch (e: CancellationException) {
            onProgress(0f)
            throw e
        }
    }
}
