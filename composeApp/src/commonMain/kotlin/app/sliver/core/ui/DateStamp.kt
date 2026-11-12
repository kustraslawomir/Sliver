package app.sliver.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.data.export.STAMP_MARGIN_DP

/** Date stamp in the bottom-right corner of the enclosing box: white mono with a soft shadow. */
@Composable
fun BoxScope.DateStamp(text: String, modifier: Modifier = Modifier) {
    SliverText(
        text,
        SliverType.Stamp.copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 4f * LocalDensity.current.density)),
        modifier.align(Alignment.BottomEnd).padding(STAMP_MARGIN_DP.dp),
        Color.White,
    )
}
