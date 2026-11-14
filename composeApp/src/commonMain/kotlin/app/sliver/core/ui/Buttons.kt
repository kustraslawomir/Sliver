package app.sliver.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton
import app.sliver.core.kit.ReelogIconBox
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

private fun Modifier.press(enabled: Boolean, onClick: () -> Unit) = composed {
    clickable(enabled = enabled, role = Role.Button, onClick = rememberTapGate(onClick))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, fullWidth: Boolean = true) =
    ReelogButton(text, onClick, modifier, ButtonKind.Primary, enabled, fullWidth)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    destructive: Boolean = false,
) = ReelogButton(text, onClick, modifier, if (destructive) ButtonKind.Destructive else ButtonKind.Secondary, enabled, fullWidth)

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) = ReelogButton(text, onClick, modifier, if (destructive) ButtonKind.Destructive else ButtonKind.Tertiary, enabled, fullWidth = false)

/** 48 dp hit area; [overCamera] puts the icon in a 44 dp glass circle. */
@Composable
fun IconButton44(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    overCamera: Boolean = false,
    content: @Composable () -> Unit,
) = ReelogIconBox(contentDescription, onClick, modifier, overCamera, content = content)

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = SliverColors.AccentText) {
    SliverText(text, SliverType.Meta, modifier, color)
}
