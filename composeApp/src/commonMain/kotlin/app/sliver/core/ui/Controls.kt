package app.sliver.core.ui

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
) {
    Box(
        modifier.heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = rememberTapGate(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (selected) SliverColors.AccentText else SliverColors.TextSecondary
        Row(
            Modifier.heightIn(min = 36.dp).clip(SliverShapes.Pill)
                .background(if (selected) SliverColors.AccentTint else Color.Transparent)
                .border(1.dp, if (selected) SliverColors.AccentLine else SliverColors.LineStrong, SliverShapes.Pill)
                .padding(horizontal = SliverSpace.S4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1),
        ) {
            if (selected) SliverIcon(SliverIcons.Check, color, size = 16.dp)
            SliverText(text, SliverType.BodySmall, Modifier.weight(1f, fill = false), maxLines = 1, softWrap = false, color = color)
            if (trailing != null) SliverIcon(trailing, color, size = 16.dp)
        }
    }
}

@Composable
fun SliverTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    BasicTextField(
        value, onValueChange,
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(SliverShapes.M)
            .background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineStrong, SliverShapes.M)
            .padding(horizontal = SliverSpace.S4),
        textStyle = SliverType.Body,
        singleLine = true,
        cursorBrush = SolidColor(SliverColors.TextPrimary),
        decorationBox = { inner ->
            Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) SliverText(placeholder, SliverType.Body, color = SliverColors.TextTertiary)
                inner()
            }
        },
    )
}

@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(SliverShapes.Pill)
            .background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.Pill)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            val bg by animateColorAsState(if (on) SliverColors.TextPrimary else Color.Transparent, SliverMotion.standard(SliverMotion.Quick))
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(SliverShapes.Pill).background(bg)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                SliverText(label, SliverType.BodySmall, color = if (on) SliverColors.OnInverse else SliverColors.TextSecondary)
            }
        }
    }
}

@Composable
fun SliverSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) SliverColors.Accent else SliverColors.LineStrong, SliverMotion.standard(SliverMotion.Quick))
    val x by animateDpAsState(if (checked) 20.dp else 2.dp, SliverMotion.standard(SliverMotion.Quick))
    Box(modifier.size(width = 44.dp, height = 26.dp).clip(CircleShape).background(track)) {
        Box(
            Modifier.offset(x, 2.dp).size(22.dp).clip(CircleShape)
                .background(if (SliverColors.light) Color.White else SliverColors.TextPrimary),
        )
    }
}

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
