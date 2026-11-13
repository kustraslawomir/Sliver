package app.sliver.feature.camera

import app.sliver.core.theme.ReelogType

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.GoldenState
import app.sliver.core.time.goldenState
import app.sliver.core.time.todayLocal
import app.sliver.feature.notifications.GoldenHourProvider
import kotlin.time.Clock
import kotlinx.coroutines.delay
import org.koin.mp.KoinPlatform

fun goldenText(state: GoldenState): String = when (state) {
    is GoldenState.Soon -> "Golden hour in ${state.minutes} min"
    is GoldenState.Now -> "Golden hour now · ${state.minutesLeft} min left"
}

/** Camera top-bar line; absent when the setting is off, location is unknown, or golden hour is not within 3 h. */
@Composable
fun GoldenHourLine(enabled: Boolean, modifier: Modifier = Modifier) {
    val provider = remember { KoinPlatform.getKoin().get<GoldenHourProvider>() }
    var state by remember { mutableStateOf<GoldenState?>(null) }
    LaunchedEffect(enabled) {
        while (enabled) {
            provider.refresh()
            state = provider.location()?.let { (lat, lon) -> goldenState(Clock.System.now(), todayLocal(), lat, lon) }
            delay(30_000)
        }
        state = null
    }
    val s = state ?: return
    val color = if (s is GoldenState.Now) SliverColors.GoldenNow else SliverColors.GoldenIdle
    Row(
        modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        SliverIcon(SliverIcons.Sun, color, size = 16.dp)
        SliverText(
            goldenText(s), SliverType.Caption.copy(fontSize = ReelogType.Size.caption, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = color, maxLines = 1, softWrap = false,
        )
    }
}
