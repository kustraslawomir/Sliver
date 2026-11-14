package app.sliver.core.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

/** Running count per tag; each log line carries it, so logd never folds repeated lines ("chatty"). */
object RecomposeCounts {
    private val counts = HashMap<String, Int>()

    fun next(tag: String): Int = (counts[tag] ?: 0).plus(1).also { counts[tag] = it }
}

/** Perf: debug builds log "tag N" per composition of [tag]; release builds do nothing (logDebug is a no-op). */
@Composable
fun RecomposeProbe(tag: String) {
    SideEffect { logDebug("Recompose", "$tag ${RecomposeCounts.next(tag)}") }
}
