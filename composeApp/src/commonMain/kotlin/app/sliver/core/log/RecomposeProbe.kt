package app.sliver.core.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

/** Perf: debug builds log one "Recompose" line per composition of [tag]; release builds do nothing (logDebug is a no-op). */
@Composable
fun RecomposeProbe(tag: String) {
    SideEffect { logDebug("Recompose", tag) }
}
