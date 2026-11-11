package app.sliver.core.ui

import androidx.activity.compose.PredictiveBackHandler as ActivityPredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlin.coroutines.cancellation.CancellationException

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
