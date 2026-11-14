package app.sliver.feature.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton
import app.sliver.core.navigation.Route
import app.sliver.core.theme.ReelogType
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
import org.koin.mp.KoinPlatform
import kotlin.time.Instant

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
        entries = runCatching {
            rank(scanner.scan(date, date, TimeZone.currentSystemDefault(), setOf(date)))
        }.getOrDefault(emptyList())
    }
    val list = entries
    val sections = remember(list, showAll) {
        list?.let { l -> dayPickerSections(l, app.sliver.core.APP_NAME).let { (mine, rest) -> mine to rest.filter {
            showAll || it.entry.camera
        } } }
    }
    val shown = sections?.second
    fun open(item: DayItem) {
        if (busy) return
        busy = true
        scope.launch {
            val route = runCatching {
                loadIntoSession(gallery, files, item.entry.uri, date, liveVideo = item.liveVideo)
            }.getOrNull()
            busy = false
            if (route == null) Notices.show("Couldn't read that item. Pick another.") else onNavigate(route)
        }
    }

    Column(Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = SliverSpace.S4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
                SliverText(
                    "${weekdayName(date)} ${date.day} ${MonthLong[date.month.ordinal]}",
                    SliverType.Title,
                    Modifier.padding(start = SliverSpace.S2),
                    maxLines = 1,
                )
            }
            Box(Modifier.height(SliverSpace.S4))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                shown == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { SlitLoader() }
                shown.isEmpty() && sections.first.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(horizontal = SliverSpace.S8).padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(SliverSpace.S2, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SliverText(
                        "Nothing from this day",
                        SliverType.Headline.copy(fontSize = ReelogType.Size.body, lineHeight = ReelogType.lineFor(ReelogType.Size.body))
                    )
                    SliverText(
                        "Your camera roll has no photos or videos from ${date.day} ${MonthLong[date.month.ordinal]}.",
                        SliverType.BodySmall.copy(textAlign = TextAlign.Center),
                        color = SliverColors.TextTertiary,
                    )
                }
                // Perf STEP 3: lazy rows (same layout: 3 tiles per row, 4 dp between children, 6 dp under each row).
                else -> androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = SliverSpace.Gutter),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Audit v1.8 §6: "Recorded in Reelog" first, then "From your camera · N"; 3 columns of 3:4 tiles.
                    if (sections.first.isNotEmpty()) {
                        item(key = "mine-title", contentType = "title") {
                            SectionTitle("Recorded in ${app.sliver.core.APP_NAME}", "full recordings")
                        }
                        tileRows("mine", sections.first, ::open)
                        item(key = "mine-gap", contentType = "gap") { Spacer(Modifier.height(SliverSpace.S3)) }
                    }
                    if (shown.isNotEmpty()) {
                        item(key = "rest-title", contentType = "title") {
                            SectionTitle(if (showAll) "From this day" else "From your camera", "${shown.size}")
                        }
                        tileRows("rest", shown, ::open)
                    }
                    item(key = "end", contentType = "gap") { Spacer(Modifier.height(SliverSpace.S4)) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
        Column(
            Modifier.padding(horizontal = SliverSpace.Gutter).padding(top = SliverSpace.S2, bottom = SliverSpace.S4),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            SwitchRow(
                "Show saved and received too",
                showAll,
                { showAll = it },
                caption = "WhatsApp, downloads, other apps. Screenshots never.",
            )
            ReelogButton("Browse all photos", { picker(date) }, kind = ButtonKind.Secondary, leading = SliverIcons.ImageIcon)
        }
    }
}

private val TileShape = RoundedCornerShape(12.dp)

@Composable
private fun SectionTitle(title: String, note: String) {
    Row(Modifier.padding(bottom = SliverSpace.S1), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SliverText(title, SliverType.BodySmall.copy(fontWeight = FontWeight.Medium), color = SliverColors.TextPrimary)
        SliverText("\u00B7 $note", SliverType.Caption, color = SliverColors.TextTertiary)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tileRows(section: String, items: List<DayItem>, onOpen: (DayItem) -> Unit) {
    val rows = items.chunked(3)
    items(rows.size, key = { "$section-$it-${rows[it].first().entry.uri}" }, contentType = { "tiles" }) { r ->
        val row = rows[r]
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { item -> DayTile(item, Modifier.weight(1f).aspectRatio(3f / 4f)) { onOpen(item) } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Tile with capture time bottom-left, video length bottom-right, motion-photo badge top-left. */
@Composable
private fun DayTile(item: DayItem, modifier: Modifier, onClick: () -> Unit) {
    val e = item.entry
    val thumb = rememberMediaThumb(e.uri)
    val time = Instant.fromEpochMilliseconds(e.takenAtMs).toLocalDateTime(TimeZone.currentSystemDefault()).time
    val clock = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    val kind = when {
        item.live -> "Live photo"
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
        val label = SliverType.Meta.copy(
            fontSize = ReelogType.Size.caption,
            lineHeight = ReelogType.lineFor(ReelogType.Size.caption),
            color = SliverColors.OnMedia
        ).overMedia()
        SliverText(clock, label, Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp))
        // Badge top-right: video length ("0:12") or "Live".
        val badge = when {
            item.live -> "Live"
            e.video -> durationLabel(e.durationMs)
            else -> null
        }
        if (badge != null) {
            SliverText(
                badge,
                label.copy(fontWeight = FontWeight.SemiBold),
                Modifier.align(Alignment.TopEnd).padding(6.dp).clip(SliverShapes.Pill).background(SliverColors.Scrim).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
