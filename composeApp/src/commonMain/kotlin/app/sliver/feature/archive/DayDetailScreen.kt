package app.sliver.feature.archive

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.SwitchRow
import app.sliver.data.export.DayShare
import app.sliver.data.export.ShareOptions
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.data.LookParams
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.looks.LookSession
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
    val koin = remember { KoinPlatform.getKoin() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val undo = remember { koin.get<SaveUndo>() }
    val dayShare = remember { koin.get<DayShare>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }

    var date by remember { mutableStateOf(ArchiveSession.selected) }
    val current = date
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)
    val day = all.firstOrNull { it.date == current }
    val filled = remember(all) { all.map { it.date } }
    val first = app?.firstDay ?: filled.minOrNull() ?: current

    var muted by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var shDate by remember(app?.dateStamp) { mutableStateOf(app?.dateStamp ?: true) }
    var shSound by remember { mutableStateOf(true) }
    var shMark by remember { mutableStateOf(false) }
    var previewLook by remember(current) { mutableStateOf<LookParams?>(null) }

    LaunchedEffect(Unit) {
        CaptureSession.path = null
        LookSession.takeApplied()?.let { applied ->
            val d = days.getDay(current) ?: return@let
            days.save(d.copy(look = applied))
            previewLook = applied
        }
    }

    fun show(target: LocalDate?) {
        if (target == null) onBack() else {
            date = target
            ArchiveSession.selected = target
        }
    }

    fun openLooks(d: app.sliver.data.db.Day) {
        CaptureSession.path = d.file
        LookSession.isPhoto = false
        LookSession.startMs = 0
        LookSession.windowMs = 1000
        LookSession.speed = 1f
        LookSession.params = d.look
        LookSession.resume = null
        onNavigate(Route.Looks)
    }

    fun replace(d: LocalDate) {
        CaptureSession.forDate = d
        onNavigate(if (d == today) Route.Camera else Route.GalleryPicker)
    }

    fun remove(d: app.sliver.data.db.Day) {
        confirm = false
        val next = nextDay(d.date, filled) ?: prevDay(d.date, filled)
        scope.launch {
            undo.remove(d, dayLabel(d.date, today))
            show(next)
        }
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
        if (day != null) {
            ClipPlayer(
                day.file, 0, 1000, Modifier.fillMaxSize(), audio = !muted, look = previewLook ?: LookParams(),
            )
            if (app?.dateStamp == true) {
                SliverText(
                    "${current.day.toString().padStart(2, '0')}.${current.month.ordinal.plus(1).toString().padStart(2, '0')}.${current.year}",
                    SliverType.MonoS,
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 56.dp, end = SliverSpace.Gutter),
                    SliverColors.AccentCore,
                )
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton44(
                        { show(prevDay(current, filled)) }, "Previous day",
                    ) {
                        SliverIcon(SliverIcons.ChevronLeft, if (prevDay(current, filled) != null) SliverColors.TextPrimary else SliverColors.TextDisabled)
                    }
                    IconButton44(
                        { nextDay(current, filled)?.let { show(it) } }, "Next day",
                    ) {
                        SliverIcon(SliverIcons.ChevronRight, if (nextDay(current, filled) != null) SliverColors.TextPrimary else SliverColors.TextDisabled)
                    }
                    IconButton44({ muted = !muted }, if (muted) "Unmute" else "Mute") {
                        SliverIcon(if (muted) SliverIcons.VolumeX else SliverIcons.Volume2, if (muted) SliverColors.TextTertiary else SliverColors.Accent)
                    }
                }
            }
            Box(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().background(SliverColors.BgVoid.copy(alpha = 0.6f))
                    .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
                verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
            ) {
                SliverText(weekdayName(current), SliverType.DisplayL)
                val time = day?.takenAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.time
                SliverText(
                    "DAY ${dayNumber(current, first)}" + (time?.let { " · ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } ?: ""),
                    SliverType.MonoS,
                )
                if (day != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        Chip(
                            day.look.preset.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                            selected = false, onClick = { openLooks(day) }, trailing = SliverIcons.ChevronDown,
                        )
                        day.place?.let { Chip(it, selected = false, onClick = {}) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    SecondaryButton("Share", { shareOpen = true }, Modifier.weight(1f), enabled = day != null)
                    SecondaryButton("Replace clip", { replace(current) }, Modifier.weight(1f))
                    GhostButton("Remove", { confirm = true }, Modifier.weight(1f), destructive = true, enabled = day != null)
                }
            }
        }

        SliverBottomSheet(shareOpen, onDismiss = { if (!sharing) shareOpen = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                SliverText("Share this clip", SliverType.DisplayM)
                SwitchRow("Date stamp", shDate, { shDate = it })
                SwitchRow("Original sound", shSound, { shSound = it })
                SwitchRow("SLIVER mark", shMark, { shMark = it })
                PrimaryButton(
                    if (sharing) "Exporting..." else "Share",
                    {
                        val d = day ?: return@PrimaryButton
                        sharing = true
                        scope.launch {
                            try {
                                dayShare.share(d.file, d.date, ShareOptions(shDate, shSound, shMark))
                                shareOpen = false
                            } finally {
                                sharing = false
                            }
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !sharing,
                )
            }
        }

        SliverBottomSheet(confirm, onDismiss = { confirm = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText("Remove this clip?", SliverType.DisplayM)
                SliverText("${dayLabel(current, today)} will be empty. You can undo for a few seconds.", SliverType.BodySmall)
                PrimaryButton("Remove", { day?.let(::remove) }, Modifier.fillMaxWidth())
                GhostButton("Keep it", { confirm = false }, Modifier.fillMaxWidth())
            }
        }
    }
}
