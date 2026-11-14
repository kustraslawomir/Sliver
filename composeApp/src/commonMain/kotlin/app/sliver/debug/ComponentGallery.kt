package app.sliver.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Badge
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SliverFrame
import app.sliver.core.ui.SliverFrameState
import app.sliver.core.ui.SliverSlider
import app.sliver.core.ui.SwitchRow

@Composable
fun ComponentGallery() {
    var chip by remember { mutableStateOf(true) }
    var seg by remember { mutableStateOf(0) }
    var sw by remember { mutableStateOf(true) }
    var slider by remember { mutableStateOf(0.4f) }
    var sheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S9),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S5),
        ) {
            SliverText("Gallery", SliverType.DisplayL)
            SliverText("Display M", SliverType.DisplayM)
            SliverText("12:04.5 mono", SliverType.Num)
            Badge("badge")
            PrimaryButton("Primary", {})
            PrimaryButton("Disabled", {}, enabled = false)
            SecondaryButton("Secondary", {})
            Row {
                GhostButton("Ghost", {})
                GhostButton("Delete", {}, destructive = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3), verticalAlignment = Alignment.CenterVertically) {
                IconButton44({}, "icon") { Box(Modifier.size(16.dp).background(SliverColors.TextPrimary)) }
                IconButton44({
                }, "icon over camera", overCamera = true) { Box(Modifier.size(16.dp).background(SliverColors.TextPrimary)) }
                Chip("Selected", chip, { chip = !chip })
                Chip("Idle", !chip, { chip = !chip })
            }
            Segmented(listOf("1s", "2s", "3s"), seg, { seg = it })
            SwitchRow("Reminder", sw, { sw = it }, caption = "Daily at 21:00")
            SliverSlider(slider, { slider = it })
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4)) {
                SliverFrame(SliverFrameState.Idle, Modifier.size(56.dp, 72.dp))
                SliverFrame(SliverFrameState.Dragging, Modifier.size(56.dp, 72.dp))
                SliverFrame(SliverFrameState.Locked, Modifier.size(56.dp, 72.dp))
            }
            SecondaryButton("Open sheet", { sheet = true })
            Box(Modifier.height(SliverSpace.S9))
        }
        SliverBottomSheet(sheet, { sheet = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S4)) {
                SliverText("Sheet title", SliverType.DisplayM)
                SliverText("Sheet body", SliverType.Body)
                PrimaryButton("Done", { sheet = false })
            }
        }
    }
}
