package app.sliver.core.ui

import app.sliver.core.theme.ReelogType

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
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

/** Default distance of the undo bar from the bottom (above the project screen's Record dock). */
val UndoBarBottomDefault: Dp = 132.dp

/** App-wide "<message> · Undo" bar; one at a time, gone after [UNDO_WINDOW_MS]. Shown by [UndoToastHost]. */
object UndoToasts {
    private val state = MutableStateFlow<UndoToast?>(null)
    val current: StateFlow<UndoToast?> = state
    private val bottomState = MutableStateFlow(UndoBarBottomDefault)
    val bottom: StateFlow<Dp> = bottomState

    fun show(message: String, onUndo: () -> Unit) {
        state.value = UndoToast(message, onUndo)
    }

    fun dismiss(toast: UndoToast) {
        state.compareAndSet(toast, null)
    }

    fun setBottom(dp: Dp) {
        bottomState.value = dp
    }
}

/** A screen whose buttons sit higher than the Record dock (Day detail) lifts the undo bar above them while shown. */
@Composable
fun UndoBarAbove(bottom: Dp) {
    DisposableEffect(bottom) {
        UndoToasts.setBottom(bottom)
        onDispose { UndoToasts.setBottom(UndoBarBottomDefault) }
    }
}

@Composable
fun UndoToastHost() {
    val toast by UndoToasts.current.collectAsState()
    val bottom by UndoToasts.bottom.collectAsState()
    val t = toast ?: return
    LaunchedEffect(t) {
        delay(UNDO_WINDOW_MS)
        UndoToasts.dismiss(t)
    }
    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(start = SliverSpace.Gutter, end = SliverSpace.Gutter, bottom = bottom),
        contentAlignment = Alignment.BottomCenter,
    ) { UndoBar(t.message, { UndoToasts.dismiss(t); t.onUndo() }) }
}

/** Design v1.5 undo bar: 48dp, radius 14, inverted colours, message left, bold "Undo" right. */
@Composable
fun UndoBar(message: String, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(SliverShapes.R14).background(SliverColors.TextPrimary).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SliverText(
            message, SliverType.Body.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), Modifier.weight(1f),
            color = SliverColors.OnInverse, maxLines = 1,
        )
        Box(Modifier.height(48.dp).clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 16.dp), Alignment.Center) {
            SliverText("Undo", SliverType.Headline.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), color = SliverColors.OnInverse)
        }
    }
}
