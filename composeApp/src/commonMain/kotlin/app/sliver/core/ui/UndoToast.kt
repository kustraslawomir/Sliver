package app.sliver.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.feature.trim.UNDO_WINDOW_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UndoToast(val message: String, val onUndo: () -> Unit)

/** App-wide "<message> · Undo" toast; one at a time, gone after [UNDO_WINDOW_MS]. Shown by [UndoToastHost]. */
object UndoToasts {
    private val state = MutableStateFlow<UndoToast?>(null)
    val current: StateFlow<UndoToast?> = state

    fun show(message: String, onUndo: () -> Unit) {
        state.value = UndoToast(message, onUndo)
    }

    fun dismiss(toast: UndoToast) {
        state.compareAndSet(toast, null)
    }
}

@Composable
fun UndoToastHost() {
    val toast by UndoToasts.current.collectAsState()
    val t = toast ?: return
    LaunchedEffect(t) {
        delay(UNDO_WINDOW_MS)
        UndoToasts.dismiss(t)
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(start = SliverSpace.Gutter, end = SliverSpace.Gutter, bottom = 104.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.clip(SliverShapes.Pill).background(SliverColors.Surface2).border(1.dp, SliverColors.LineStrong, SliverShapes.Pill)
                .padding(start = SliverSpace.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SliverText(t.message, SliverType.BodySmall, color = SliverColors.TextPrimary, maxLines = 1)
            GhostButton("Undo", { UndoToasts.dismiss(t); t.onUndo() })
        }
    }
}
