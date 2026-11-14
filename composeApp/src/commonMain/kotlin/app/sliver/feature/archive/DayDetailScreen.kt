package app.sliver.feature.archive

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton

import app.sliver.core.theme.ReelogType

import app.sliver.feature.gallery.rememberAddMedia
import app.sliver.core.ui.DateStamp
import app.sliver.data.export.stampText
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.ConfirmSheet
import app.sliver.core.ui.SwitchRow
import app.sliver.feature.autoimport.AutoImportStore
import app.sliver.feature.autoimport.CandidateTile
import app.sliver.feature.autoimport.DraftActions
import app.sliver.feature.autoimport.dismissKey
import app.sliver.feature.autoimport.encodeCandidates
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import app.sliver.data.DaySource
import app.sliver.core.ui.UndoBarAbove
import androidx.compose.ui.unit.sp
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.NoticeSheet
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
import app.sliver.data.files.FileStore
import app.sliver.data.db.Day
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.trim.ClipPlayer
import app.sliver.feature.trim.dayLabel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform

@Composable
fun DayDetailScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    app.sliver.core.log.RecomposeProbe("day")
    val koin = remember { KoinPlatform.getKoin() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val undo = remember { koin.get<SaveUndo>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }

    var date by remember { mutableStateOf(ArchiveSession.selected) }
    val current = date
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val all by remember { days.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val app by remember { settings.settings }.collectAsStateWithLifecycle(null)
    val day = all.firstOrNull { it.date == current }
    val filled = remember(all) { all.map { it.date } }
    val first = app?.firstDay ?: filled.minOrNull() ?: current

    var muted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        CaptureSession.path = null
    }

    fun show(target: LocalDate?) {
        if (target == null) onBack() else {
            date = target
            ArchiveSession.selected = target
        }
    }

    val addMedia = rememberAddMedia(onNavigate)

    val files = remember { koin.get<FileStore>() }
    val original = day?.original?.takeIf { files.exists(it) }

    /** T-glass-v1.7 10: pick a different 1 s from the full in-app recording; the recording itself is never changed. */
    fun changeMoment(d: Day, path: String) {
        CaptureSession.path = path
        CaptureSession.source = DaySource.CAMERA
        CaptureSession.forDate = d.date
        CaptureSession.changeMoment = d
        onNavigate(Route.Trim)
    }

    fun replace(d: LocalDate) {
        CaptureSession.forDate = d
        if (d == today) onNavigate(Route.Camera) else addMedia(d)
    }

    var notice by remember { mutableStateOf<String?>(null) }
    val importStore = remember { koin.get<AutoImportStore>() }
    val draftActions = remember { koin.get<DraftActions>() }
    val altMap by remember { importStore.alternatives }.collectAsStateWithLifecycle(emptyMap())
    val others = day?.let { altMap[dismissKey(it.projectId, it.date)] }.orEmpty()

    fun remove(d: app.sliver.data.db.Day) {
        val next = nextDay(d.date, filled) ?: prevDay(d.date, filled)
        scope.launch {
            if (runCatching { undo.remove(d) }.isFailure) notice = "Couldn't remove this clip. Try again." else show(next)
        }
    }

    UndoBarAbove(200.dp)
    Box(Modifier.fillMaxSize().background(SliverColors.Media)) {
        if (day != null) ClipPlayer(day.file, 0, 1000, Modifier.fillMaxSize(), audio = !muted, fill = true)
        // v1.7 item 8: scrims so text reads over bright video; no date stamp here (the title is the date).
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
        Box(
            // DayV18: the scrim carries the title, arrows and tiles, so it ends near black.
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(380.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Black.copy(alpha = 0.7f), 1f to Color.Black.copy(alpha = 0.95f))),
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SliverSpace.S3, vertical = SliverSpace.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton44(onBack, "Back", overCamera = true) { SliverIcon(SliverIcons.ChevronLeft, SliverColors.OnMedia) }
            IconButton44({ muted = !muted }, if (muted) "Sound off" else "Sound on", overCamera = true) {
                SliverIcon(if (muted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.OnMedia)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = SliverSpace.Gutter).padding(bottom = SliverSpace.S4),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S6),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val prev = prevDay(current, filled)
                val next = nextDay(current, filled)
                // DayV18: plain arrows, the scrim carries them.
                IconButton44({ show(prev) }, "Previous day") {
                    SliverIcon(SliverIcons.ChevronLeft, SliverColors.OnMedia.copy(alpha = if (prev != null) 1f else 0.4f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    SliverText(
                        "${weekdayName(current)} ${current.day} ${MonthLong[current.month.ordinal]}",
                        ReelogType.title.copy(fontSize = ReelogType.Size.dayTitle, lineHeight = ReelogType.lineFor(ReelogType.Size.dayTitle)), color = SliverColors.OnMedia, maxLines = 1,
                    )
                    val time = day?.takenAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.time
                    val whenText = if (current == today) "Today" else "Day ${dayNumber(current, first)}"
                    SliverText(
                        whenText + (time?.let { " · ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } ?: ""),
                        ReelogType.label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"), color = SliverColors.DayMeta,
                    )
                }
                IconButton44({ next?.let { show(it) } }, "Next day") {
                    SliverIcon(SliverIcons.ChevronRight, SliverColors.OnMedia.copy(alpha = if (next != null) 1f else 0.4f))
                }
            }
            if (day != null && others.isNotEmpty()) {
                // Owner: files added later (e.g. from an iPhone) can replace a saved day's clip.
                SliverText("Other items from this day", SliverType.Body.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), color = SliverColors.OnMedia)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    items(others.size) { i ->
                        val c = others[i]
                        CandidateTile(c, false, Modifier.size(64.dp)) {
                            scope.launch {
                                val target = day.copy(draftCandidates = encodeCandidates(others))
                                val route = draftActions.open(target, c.uri, chain = false)
                                if (route == null) notice = "Couldn't read that item. Pick another." else onNavigate(route)
                            }
                        }
                    }
                }
            }
            // DayV18: three equal tiles — Moment · Replace · Remove (Remove keeps the undo bar, no dialog).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (day != null && original != null) {
                    ActionTile(SliverIcons.Scissors, "Moment", SliverColors.OnMedia, Modifier.weight(1f)) { changeMoment(day, original) }
                }
                ActionTile(SliverIcons.Swap, "Replace", SliverColors.OnMedia, Modifier.weight(1f)) { replace(current) }
                ActionTile(SliverIcons.Trash2, "Remove", SliverColors.DestructiveFill, Modifier.weight(1f), enabled = day != null) { day?.let(::remove) }
            }
        }

        NoticeSheet(notice) { notice = null }
    }
}

/** DayV18 action tile: 72 dp, radius 20, white 10 % over the scrim, icon 24 + label 14/500. */
@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier.height(72.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.10f))
            .clickable(enabled = enabled, role = Role.Button, onClick = app.sliver.core.ui.rememberTapGate(onClick))
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        SliverIcon(icon, tint, size = 24.dp)
        SliverText(label, ReelogType.label.copy(fontSize = ReelogType.Size.tileLabel, fontWeight = FontWeight.Medium), color = tint, maxLines = 1)
    }
}
