package app.sliver.feature.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

/** A quiet line at the top of the calendar while a backup sync runs. Owner 13 Nov: pull-to-sync removed (glitchy). */
@Composable
fun SyncStrip(syncing: Boolean) {
    if (!syncing) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SliverIcon(SliverIcons.CloudUpload, SliverColors.TextTertiary, size = 16.dp)
        SliverText("Syncing", SliverType.Caption, color = SliverColors.TextTertiary)
    }
}
