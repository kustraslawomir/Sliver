package app.sliver.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.designsystem.Glow
import app.sliver.core.designsystem.glow
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

private fun Modifier.press(enabled: Boolean, onClick: () -> Unit) = composed {
    clickable(enabled = enabled, role = Role.Button, onClick = rememberTapGate(onClick))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, fullWidth: Boolean = true) {
    Box(
        modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier).height(48.dp)
            .then(if (enabled) Modifier.glow(SliverShapes.Pill, *Glow.cta) else Modifier)
            .clip(SliverShapes.Pill)
            .background(if (enabled) SliverColors.Accent else SliverColors.Surface3)
            .press(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        SliverText(text, SliverType.Headline, Modifier.padding(horizontal = SliverSpace.S4), color = if (enabled) SliverColors.OnAccent else SliverColors.TextTertiary, maxLines = 1, softWrap = false)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, fullWidth: Boolean = true) {
    Box(
        modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier).height(48.dp)
            .clip(SliverShapes.Pill)
            .border(1.dp, SliverColors.LineStrong, SliverShapes.Pill)
            .press(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        SliverText(text, SliverType.Headline, Modifier.padding(horizontal = SliverSpace.S4), color = if (enabled) SliverColors.TextPrimary else SliverColors.TextDisabled, maxLines = 1, softWrap = false)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier.heightIn(min = 48.dp).clip(SliverShapes.Pill).press(enabled, onClick).padding(horizontal = SliverSpace.S4),
        contentAlignment = Alignment.Center,
    ) {
        val color = when {
            !enabled -> SliverColors.TextDisabled
            destructive -> SliverColors.Error
            else -> SliverColors.TextSecondary
        }
        SliverText(text, SliverType.Headline, color = color, maxLines = 1, softWrap = false)
    }
}

/** 48x48 hit area; [overCamera] adds the void scrim behind the icon. */
@Composable
fun IconButton44(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    overCamera: Boolean = false,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.size(48.dp).clip(CircleShape)
            .then(if (overCamera) Modifier.background(SliverColors.BgVoid.copy(alpha = 0.5f)) else Modifier)
            .semantics { this.contentDescription = contentDescription }
            .clickable(source, null, role = Role.Button, onClick = rememberTapGate(onClick)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = SliverColors.AccentText) {
    SliverText(text, SliverType.Meta, modifier, color)
}
