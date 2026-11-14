package app.sliver.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val ItemH = 48.dp
private const val Visible = 5

/**
 * Audit v1.8 §7 wheel (board phone 3): 5 rows, the middle one selected (40/600 tabular, band white 7 %), the rest dimmer.
 * Snaps to a row; reports the row when scrolling stops; follows [index] when it changes from outside.
 */
@Composable
fun Wheel(items: List<String>, index: Int, onIndex: (Int) -> Unit, label: String, modifier: Modifier = Modifier, onLongPress: (() -> Unit)? = null) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(state)
    val itemPx = with(LocalDensity.current) { ItemH.toPx() }
    val centred by remember { derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0 } }
    val report by rememberUpdatedState(onIndex)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centred }.filter { !it.first }.distinctUntilChanged().collect { report(it.second.coerceIn(0, items.size - 1)) }
    }
    LaunchedEffect(index, items.size) { if (index != centred && !state.isScrollInProgress) state.animateScrollToItem(index.coerceIn(0, items.size - 1)) }
    Box(modifier.height(ItemH * Visible).semantics { contentDescription = "$label ${items.getOrNull(centred).orEmpty()}" }, Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(ItemH).background(SliverColors.TextPrimary.copy(alpha = 0.07f), RoundedCornerShape(12.dp)))
        LazyColumn(
            Modifier.fillMaxWidth().height(ItemH * Visible)
                .then(if (onLongPress != null) Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) } else Modifier),
            state = state, flingBehavior = fling, contentPadding = PaddingValues(vertical = ItemH * (Visible / 2)),
        ) {
            itemsIndexed(items) { i, text ->
                val on = i == centred
                Box(Modifier.fillMaxWidth().height(ItemH), Alignment.Center) {
                    SliverText(
                        text,
                        (if (on) ReelogType.display.copy(fontSize = ReelogType.Size.display) else ReelogType.title).copy(fontFeatureSettings = "tnum", textAlign = TextAlign.Center),
                        color = if (on) SliverColors.TextPrimary else SliverColors.TextDisabled,
                    )
                }
            }
        }
    }
}
