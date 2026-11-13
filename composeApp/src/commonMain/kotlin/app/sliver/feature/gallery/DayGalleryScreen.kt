package app.sliver.feature.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.Notices
import app.sliver.core.ui.SlitLoader
import app.sliver.core.ui.SwitchRow
import app.sliver.data.files.FileStore
import app.sliver.feature.archive.MonthLong
import app.sliver.feature.archive.weekdayName
import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.autoimport.MediaScanner
import app.sliver.feature.autoimport.durationLabel
import app.sliver.feature.autoimport.rank
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.camera.overMedia
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.koin.mp.KoinPlatform

/** Board FromThisDay: every photo and video taken on [CaptureSession.forDate] (camera first, never screenshots). */
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
    val list = entries
    val shown = list?.let { l -> if (showAll) l else l.filter { it.camera } }

    Column(Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = SliverSpace.S4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
                SliverText(
                    "${weekdayName(date)} ${date.day} ${MonthLong[date.month.ordinal]}", SliverType.Title,
                    Modifier.padding(start = SliverSpace.S2), maxLines = 1,
                )
            }
            SliverText(
                when {
                    shown.isNullOrEmpty() -> "From your camera roll"
                    showAll -> "${shown.size} from this day"
                    else -> "${shown.size} from your camera"
                },
                SliverType.BodySmall.copy(fontSize = 15.sp, lineHeight = 20.sp), Modifier.padding(top = SliverSpace.S2, bottom = SliverSpace.S4),
                color = SliverColors.TextTertiary,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                shown == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { SlitLoader() }
                shown.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(horizontal = SliverSpace.S8).padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(SliverSpace.S2, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SliverText("Nothing from this day", SliverType.Headline.copy(fontSize = 17.sp, lineHeight = 22.sp))
                    SliverText(
                        "Your camera roll has no photos or videos from ${date.day} ${MonthLong[date.month.ordinal]}.",
                        SliverType.BodySmall.copy(textAlign = TextAlign.Center), color = SliverColors.TextTertiary,
                    )
                }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = SliverSpace.Gutter),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    shown.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { e ->
                                DayTile(e, Modifier.weight(1f).aspectRatio(1f)) {
                                    if (busy) return@DayTile
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
                    Spacer(Modifier.height(SliverSpace.S4))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
        Column(
            Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = SliverSpace.S2, bottom = SliverSpace.S4),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            SwitchRow(
                "Show saved and received too", showAll, { showAll = it },
                caption = "WhatsApp, downloads, other apps. Screenshots never.",
            )
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(SliverShapes.Pill).border(1.dp, SliverColors.LineStrong, SliverShapes.Pill)
                    .clickable(role = Role.Button) { picker(date) },
                horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliverIcon(SliverIcons.ImageIcon, SliverColors.TextPrimary, size = 18.dp)
                SliverText("Browse all photos", SliverType.Body.copy(fontSize = 16.sp), color = SliverColors.TextPrimary)
            }
        }
    }
}

private val TileShape = RoundedCornerShape(10.dp)

/** Tile with capture time bottom-left, video length bottom-right, motion-photo badge top-left. */
@Composable
private fun DayTile(e: MediaEntry, modifier: Modifier, onClick: () -> Unit) {
    val thumb = rememberMediaThumb(e.uri)
    val time = Instant.fromEpochMilliseconds(e.takenAtMs).toLocalDateTime(TimeZone.currentSystemDefault()).time
    val clock = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    val kind = when {
        e.motion || e.live -> "Moving photo"
        e.video -> "Video, ${durationLabel(e.durationMs)}"
        else -> "Photo"
    }
    Box(
        modifier.clip(TileShape).background(SliverColors.Surface3)
            .semantics { contentDescription = "$kind, $clock" }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(36.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, SliverColors.Scrim))),
        )
        val label = SliverType.Meta.copy(fontSize = 11.sp, lineHeight = 14.sp, color = SliverColors.OnMedia).overMedia()
        SliverText(clock, label, Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp))
        if (e.video && !e.motion && !e.live) {
            SliverText(
                durationLabel(e.durationMs), label.copy(fontWeight = FontWeight.SemiBold),
                Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 5.dp),
            )
        }
        if (e.motion || e.live) SliverIcon(SliverIcons.CircleDot, SliverColors.OnMedia, Modifier.align(Alignment.TopStart).padding(6.dp), size = 16.dp)
    }
}
