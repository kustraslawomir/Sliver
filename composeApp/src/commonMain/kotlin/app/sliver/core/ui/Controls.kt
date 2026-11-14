package app.sliver.core.ui

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import app.sliver.core.theme.ReelogType

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverMotion
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: ImageVector? = null,
) = app.sliver.core.kit.ReelogChip(text, selected, onClick, modifier, trailing = trailing)

/** Audit v1.8 §7 text input: 56 dp, radius 16, #111113 fill, 1 dp edge (#3A3A40, focused #EDEDE8), clear button. */
@Composable
fun SliverTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    autoFocus: Boolean = false,
) {
    val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (autoFocus) androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    BasicTextField(
        value, onValueChange,
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .background(SliverColors.FieldFill)
            .border(1.dp, if (focused) SliverColors.FieldFocus else SliverColors.FieldEdge, shape)
            .focusRequester(focus).onFocusChanged { focused = it.isFocused }
            .padding(start = SliverSpace.S4),
        textStyle = SliverType.Body,
        singleLine = true,
        cursorBrush = SolidColor(SliverColors.TextPrimary),
        decorationBox = { inner ->
            Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) SliverText(placeholder, SliverType.Body, color = SliverColors.TextTertiary)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier.size(48.dp).clickable(role = androidx.compose.ui.semantics.Role.Button) { onValueChange("") }
                            .semantics { contentDescription = "Clear" },
                        Alignment.Center,
                    ) { SliverIcon(SliverIcons.X, SliverColors.TextTertiary, size = 18.dp) }
                }
            }
        },
    )
}

@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) =
    app.sliver.core.kit.ReelogSegmented(options, selectedIndex, onSelect, modifier)

@Composable
fun SliverSwitch(checked: Boolean, modifier: Modifier = Modifier) = app.sliver.core.kit.ReelogSwitch(checked, modifier)

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp)
            .semantics { toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Column(Modifier.weight(1f)) {
            SliverText(title, SliverType.Body)
            if (caption != null) SliverText(caption, SliverType.Caption)
        }
        SliverSwitch(checked)
    }
}

@Composable
fun SliverSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    var widthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val thumbW = 3.dp
    fun snap(v: Float): Float {
        val c = v.coerceIn(0f, 1f)
        return if (steps > 0) (c * (steps + 1)).let { kotlin.math.round(it) / (steps + 1) } else c
    }
    fun fromX(x: Float) = if (widthPx > 0) snap(x / widthPx) else value

    Box(
        modifier.fillMaxWidth().height(48.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f, steps)
                setProgress { onValueChange(snap(it)); true }
            }
            .pointerInput(steps) { detectTapGestures { onValueChange(fromX(it.x)) } }
            .pointerInput(steps) { detectDragGestures { change, _ -> onValueChange(fromX(change.position.x)) } },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(SliverColors.LineStrong))
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(2.dp).background(SliverColors.Accent))
        val thumbX = with(density) { (widthPx * value.coerceIn(0f, 1f)).toDp() } - thumbW / 2
        Box(Modifier.offset(thumbX.coerceAtLeast(0.dp), 0.dp).size(thumbW, 24.dp).clip(SliverShapes.Xs).background(SliverColors.AccentCore))
    }
}
