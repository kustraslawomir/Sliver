package app.sliver.core

import app.sliver.core.log.logDebug
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The one process-wide scope for work that outlives a screen; failures are logged instead of crashing the app. */
object AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> logDebug("AppScope", "uncaught: $e") },
)
