package app.sliver.feature.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.Notices
import app.sliver.core.ui.SlitLoader
import app.sliver.core.ui.SwitchRow
import app.sliver.data.files.FileStore
import app.sliver.feature.autoimport.CandidateTile
import app.sliver.feature.autoimport.DraftCandidate
import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.autoimport.MediaScanner
import app.sliver.feature.autoimport.dayItemsTitle
import app.sliver.feature.autoimport.rank
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.mp.KoinPlatform

/** Every photo and video taken on [CaptureSession.forDate] (camera first, never screenshots), to add or replace that day. */
@Composable
fun DayGalleryScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val scanner = remember { koin.get<MediaScanner>() }
    val gallery = remember { koin.get<GalleryProvider>() }
    val files = remember { koin.get<FileStore>() }
    val scope = rememberCoroutineScope()
    val date = remember { CaptureSession.forDate } ?: return onBack()
    val picker = rememberAddMedia(onNavigate, preferDayGallery = false)

    var entries by remember { mutableStateOf<List<MediaEntry>?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(date) {
        entries = runCatching { rank(scanner.scan(date, date, TimeZone.currentSystemDefault(), setOf(date))) }.getOrDefault(emptyList())
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            val all = entries
            val shownCount = all?.let { list -> if (showAll || list.none { it.camera }) list.size else list.count { it.camera } } ?: 0
            SliverText(dayItemsTitle(date, shownCount.coerceAtLeast(1)), SliverType.Title, Modifier.padding(start = SliverSpace.S2))
        }
        val list = entries
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(vertical = SliverSpace.S6), Alignment.Center) { SlitLoader() }
            list.isEmpty() -> SliverText("Nothing from this day in your gallery.", SliverType.Body, color = SliverColors.TextSecondary)
            else -> {
                val hasOther = list.any { !it.camera } && list.any { it.camera }
                if (hasOther) SwitchRow("Show saved and received too", showAll, { showAll = it })
                val shown = if (showAll || list.none { it.camera }) list else list.filter { it.camera }
                SliverText("Tap one to use it for this day.", SliverType.BodySmall, color = SliverColors.TextSecondary)
                shown.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        row.forEach { e ->
                            CandidateTile(DraftCandidate(e.uri, e.video, e.durationMs, e.motion), false, Modifier.weight(1f).aspectRatio(1f)) {
                                if (busy) return@CandidateTile
                                busy = true
                                scope.launch {
                                    val route = runCatching { loadIntoSession(gallery, files, e.uri, date) }.getOrNull()
                                    busy = false
                                    if (route == null) Notices.show("Couldn't read that item. Pick another.") else onNavigate(route)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        GhostButton("Browse all photos", { picker(date) }, Modifier.fillMaxWidth())
    }
}
