package app.sliver.feature.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType

/** Pull down past the top of the list to run a sync; only counts while the list is already at its top. */
class PullToSync(private val thresholdPx: Float, private val enabled: () -> Boolean, private val onTrigger: () -> Unit) {
    var pull by mutableFloatStateOf(0f)
        private set

    val armed get() = pull >= thresholdPx

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (pull > 0f && available.y < 0f) {
                val used = maxOf(available.y, -pull)
                pull += used
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (enabled() && source == NestedScrollSource.UserInput && available.y > 0f) {
                pull = (pull + available.y * 0.5f).coerceAtMost(thresholdPx * 1.5f)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (armed) onTrigger()
            pull = 0f
            return Velocity.Zero
        }
    }
}

/** What the pull is doing: a hint while pulling, a line while a sync runs. */
@Composable
fun SyncStrip(armed: Boolean, pulling: Boolean, syncing: Boolean) {
    if (!armed && !pulling && !syncing) return
    val label = when {
        syncing -> "Syncing"
        armed -> "Release to sync"
        else -> "Pull to sync"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SliverIcon(SliverIcons.CloudUpload, SliverColors.TextTertiary, size = 16.dp)
        SliverText(label, SliverType.Caption, color = SliverColors.TextTertiary)
    }
}

@Composable
fun rememberPullToSync(thresholdPx: Float, enabled: () -> Boolean, onTrigger: () -> Unit): PullToSync =
    remember(thresholdPx) { PullToSync(thresholdPx, enabled, onTrigger) }
