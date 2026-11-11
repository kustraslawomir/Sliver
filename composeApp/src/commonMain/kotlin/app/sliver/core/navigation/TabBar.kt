package app.sliver.core.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverGlow
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

@Composable
fun SliverTabBar(current: Route, onSelect: (Route) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4)
            .fillMaxWidth().height(SliverSpace.S10)
            .background(SliverColors.Surface2, SliverShapes.Pill)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.Pill),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem("Archive", current == Route.Archive) { onSelect(Route.Archive) }
        CameraSlitButton { onSelect(Route.Camera) }
        TabItem("Films", current == Route.Films) { onSelect(Route.Films) }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 96.dp, height = SliverSpace.S10).clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SliverText(
            label.uppercase(),
            SliverType.MonoS,
            color = if (selected) SliverColors.Accent else SliverColors.TextTertiary,
        )
    }
}

@Composable
private fun CameraSlitButton(onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).border(3.dp, SliverColors.TextPrimary, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Open camera", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 4.dp, height = 24.dp).then(SliverGlow.small()).background(SliverColors.Accent))
    }
}
