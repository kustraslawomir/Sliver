package app.sliver.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAP_LOCKOUT_MS = 600L

/** Drops taps that arrive within a short window of the previous accepted tap. */
@Stable
class TapGate(private val lockoutMs: Long = TAP_LOCKOUT_MS) {
    private var last: TimeSource.Monotonic.ValueTimeMark? = null

    fun accept(): Boolean {
        val l = last
        if (l != null && l.elapsedNow().inWholeMilliseconds < lockoutMs) return false
        last = TimeSource.Monotonic.markNow()
        return true
    }
}

@Composable
fun rememberTapGate(onClick: () -> Unit): () -> Unit {
    val gate = remember { TapGate() }
    val latest by rememberUpdatedState(onClick)
    return { if (gate.accept()) latest() }
}

/** Runs one async action at a time; [running] drives the disabled and progress state of the button. */
@Stable
class SingleFlight {
    var running by mutableStateOf(false)
        private set

    fun launch(scope: CoroutineScope, block: suspend () -> Unit) {
        if (running) return
        running = true
        scope.launch {
            try {
                block()
            } finally {
                running = false
            }
        }
    }
}

@Composable
fun rememberSingleFlight() = remember { SingleFlight() }
