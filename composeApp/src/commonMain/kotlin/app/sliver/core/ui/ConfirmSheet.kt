package app.sliver.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sliver.core.kit.GlassStyle
import app.sliver.core.kit.glass
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverText

/**
 * Audit v1.8 §7 dialog (board phone 1): strong glass card, radius 28, 24 dp padding, title 22/600, body 15, a filled
 * destructive button (#E5675F, dark text) over a Cancel text button; scrim black 55 %. Name kept so every caller gets it.
 */
@Composable
fun ConfirmSheet(visible: Boolean, title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DisposableEffect(visible) {
        if (visible) SheetsOpen.count++
        onDispose { if (visible) SheetsOpen.count-- }
    }
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            Alignment.Center,
        ) {
            AnimatedVisibility(visible, enter = scaleIn(initialScale = 0.94f) + fadeIn(), exit = scaleOut(targetScale = 0.96f) + fadeOut()) {
                Column(
                    Modifier.padding(horizontal = 24.dp).widthIn(max = 360.dp).fillMaxWidth()
                        .glass(GlassStyle.Strong, DialogShape)
                        .clickable(remember { MutableInteractionSource() }, null) {}
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SliverText(title, ReelogType.headline)
                    SliverText(body, ReelogType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = SliverColors.DialogBody)
                    Box(
                        Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
                            .background(SliverColors.DestructiveFill)
                            .clickable(role = Role.Button, onClick = rememberTapGate { onDismiss(); onConfirm() }),
                        Alignment.Center,
                    ) { SliverText(confirm, ReelogType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = SliverColors.OnDestructive) }
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onDismiss),
                        Alignment.Center,
                    ) { SliverText("Cancel", ReelogType.body, color = SliverColors.TextPrimary) }
                }
            }
        }
    }
}

private val DialogShape = RoundedCornerShape(28.dp)
