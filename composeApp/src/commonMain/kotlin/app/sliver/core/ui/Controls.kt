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
import androidx.compose.runtime.Composable
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
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 36.dp).clip(SliverShapes.Pill)
            .background(if (selected) SliverColors.AccentTint else Color.Transparent)
            .border(1.dp, if (selected) SliverColors.AccentLine else SliverColors.LineHairline, SliverShapes.Pill)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SliverSpace.S4),
        contentAlignment = Alignment.Center,
    ) {
        SliverText(text, SliverType.BodySmall, color = if (selected) SliverColors.Accent else SliverColors.TextSecondary)
    }
}

@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(SliverShapes.Pill)
            .background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.Pill)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            val bg by animateColorAsState(if (on) SliverColors.TextPrimary else Color.Transparent, SliverMotion.standard(SliverMotion.Quick))
            Box(
                Modifier.weight(1f).height(36.dp).clip(SliverShapes.Pill).background(bg)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                SliverText(label, SliverType.BodySmall, color = if (on) SliverColors.OnAccent else SliverColors.TextSecondary)
            }
        }
    }
}

@Composable
fun SliverSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) SliverColors.Accent else SliverColors.Surface3, SliverMotion.standard(SliverMotion.Quick))
    val x by animateDpAsState(if (checked) 20.dp else 2.dp, SliverMotion.standard(SliverMotion.Quick))
    Box(modifier.size(width = 48.dp, height = 28.dp).clip(CircleShape).background(track)) {
        Box(
            Modifier.offset(x, 2.dp).size(24.dp).clip(CircleShape)
                .background(if (checked) SliverColors.OnAccent else SliverColors.TextSecondary),
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
        modifier.fillMaxWidth().height(32.dp)
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
