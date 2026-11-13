package app.sliver.core.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverText
import app.sliver.core.ui.rememberTapGate

/**
 * v1.7 controls (design/png/GlassKit.png §4–6). Own composables, no Material. Ink = the theme's primary text colour, so
 * "white 6 %" on dark becomes "ink 6 %" on the light theme.
 */
private val ink get() = SliverColors.TextPrimary
private val Pill = RoundedCornerShape(percent = 50)

enum class ButtonKind { Primary, Secondary, Tertiary, Destructive }

/** Press feedback shared by the kit: scale down, no ripple. */
@Composable
private fun pressScale(source: MutableInteractionSource, pressed: Float): Float {
    val down by source.collectIsPressedAsState()
    return animateFloatAsState(if (down) pressed else 1f, tween(100), label = "press").value
}

@Composable
fun ReelogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    small: Boolean = false,
    leading: ImageVector? = null,
    overMedia: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.97f)
    val (fill, content) = when (kind) {
        ButtonKind.Primary -> (if (enabled) SliverColors.Accent else ink.copy(alpha = 0.08f)) to
            (if (enabled) SliverColors.OnAccent else SliverColors.TextDisabled)
        ButtonKind.Secondary -> Color.Transparent to (if (enabled) ink else SliverColors.TextDisabled)
        ButtonKind.Tertiary -> Color.Transparent to (if (enabled) SliverColors.TextSecondary else SliverColors.TextDisabled)
        ButtonKind.Destructive -> Color.Transparent to (if (enabled) SliverColors.Error else SliverColors.TextDisabled)
    }
    val surface = when {
        kind == ButtonKind.Destructive && overMedia -> Modifier.glass(GlassStyle.Regular, Pill, overMedia = true)
        kind != ButtonKind.Secondary -> Modifier.background(fill, Pill)
        overMedia -> Modifier.glass(GlassStyle.Regular, Pill, overMedia = true)
        else -> Modifier.background(ink.copy(alpha = 0.06f), Pill).border(1.dp, ink.copy(alpha = 0.12f), Pill)
    }
    Row(
        modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (small) 44.dp else 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(Pill)
            .then(surface)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = rememberTapGate(onClick))
            .padding(horizontal = if (small) 18.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) SliverIcon(leading, content, size = if (small) 18.dp else 20.dp)
        val style = if (small) ReelogType.label else ReelogType.body.copy(fontWeight = if (kind == ButtonKind.Primary) FontWeight.SemiBold else FontWeight.Medium)
        SliverText(text, style, color = content, maxLines = 1, softWrap = false)
    }
}

/** 40 dp pill chip in a 48 dp touch target. */
@Composable
fun ReelogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.97f)
    val content = if (selected) SliverColors.OnAccent else ink
    Box(
        modifier.heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .clickable(source, null, role = Role.Button, onClick = rememberTapGate(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.heightIn(min = 40.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(Pill)
                .then(
                    if (selected) Modifier.background(SliverColors.Accent)
                    else Modifier.background(ink.copy(alpha = 0.06f)).border(1.dp, ink.copy(alpha = 0.12f), Pill),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leading != null) SliverIcon(leading, content, size = 18.dp)
            SliverText(text, ReelogType.label, Modifier.weight(1f, fill = false), maxLines = 1, softWrap = false, color = content)
            if (trailing != null) SliverIcon(trailing, content, size = 18.dp)
        }
    }
}

/** 44 dp track, sliding thumb. Month/Year, Sound, Date style, Video/Photo. */
@Composable
fun ReelogSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    overMedia: Boolean = false,
) {
    BoxWithConstraints(
        modifier.fillMaxWidth().height(44.dp).clip(Pill)
            .then(
                if (overMedia) Modifier.glass(GlassStyle.Regular, Pill, overMedia = true)
                else Modifier.background(ink.copy(alpha = 0.06f)).border(1.dp, ink.copy(alpha = 0.10f), Pill),
            )
            .padding(3.dp),
    ) {
        val n = options.size.coerceAtLeast(1)
        val w = maxWidth / n
        val x by animateDpAsState(w * selectedIndex.coerceIn(0, n - 1), tween(220), label = "thumb")
        if (selectedIndex in options.indices) {
            Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(Pill).background(SliverColors.Accent))
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val on = i == selectedIndex
                val color by animateColorAsState(if (on) SliverColors.OnAccent else SliverColors.TextSecondary, tween(220), label = "seg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(Pill)
                        .semantics { selected = on }
                        .clickable(role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { SliverText(label, ReelogType.label, color = color, maxLines = 1, softWrap = false) }
            }
        }
    }
}

/** Reminder weekday: 44 dp circle, filled when on, ring when off. */
@Composable
fun DayToggle(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, description: String = label) {
    Box(
        modifier.size(48.dp).semantics { selected = on; contentDescription = description }
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .then(if (on) Modifier.background(SliverColors.Accent) else Modifier.border(1.dp, SliverColors.LineStrong, CircleShape)),
            contentAlignment = Alignment.Center,
        ) { SliverText(label, ReelogType.label, color = if (on) SliverColors.OnAccent else SliverColors.TextTertiary) }
    }
}

/** 52×32 switch; the row around it is the touch target. */
@Composable
fun ReelogSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val light = SliverColors.light
    val offTrack = if (light) Color(0xFFDCDAD3) else Color(0xFF2C2C30)
    val offKnob = if (light) Color.White else SliverColors.TextTertiary
    val track by animateColorAsState(if (checked) SliverColors.Accent else offTrack, tween(200), label = "track")
    val knob by animateColorAsState(if (checked) SliverColors.OnAccent else offKnob, tween(200), label = "knob")
    val x by animateDpAsState(if (checked) 23.dp else 3.dp, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium), label = "x")
    Box(
        modifier.size(width = 52.dp, height = 32.dp).clip(CircleShape).background(track)
            .then(if (checked) Modifier else Modifier.border(1.dp, ink.copy(alpha = 0.10f), CircleShape)),
    ) {
        Box(Modifier.offset(x, 3.dp).size(26.dp).clip(CircleShape).background(knob))
    }
}

/**
 * 48 dp hit area around a 24 dp icon. Plain screens: no background. [overMedia]: 44 dp regular-glass circle, for
 * controls over video or the camera.
 */
@Composable
fun ReelogIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overMedia: Boolean = false,
    tint: Color = if (overMedia) SliverColors.OnMedia else SliverColors.TextPrimary,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    ReelogIconBox(contentDescription, onClick, modifier, overMedia, enabled) { SliverIcon(icon, if (enabled) tint else SliverColors.TextDisabled, size = iconSize) }
}

@Composable
fun ReelogIconBox(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overMedia: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, 0.94f)
    Box(
        modifier.size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = rememberTapGate(onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .then(if (overMedia) Modifier.glass(GlassStyle.Regular, CircleShape, overMedia = true) else Modifier),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
