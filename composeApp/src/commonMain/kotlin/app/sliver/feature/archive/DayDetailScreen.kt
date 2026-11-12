package app.sliver.feature.archive

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
import app.sliver.core.ui.NoticeSheet
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.CaptureSession
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

    fun replace(d: LocalDate) {
        CaptureSession.forDate = d
        if (d == today) onNavigate(Route.Camera) else addMedia(d)
    }

    var notice by remember { mutableStateOf<String?>(null) }

    fun remove(d: app.sliver.data.db.Day) {
        val next = nextDay(d.date, filled) ?: prevDay(d.date, filled)
        scope.launch {
            if (runCatching { undo.remove(d) }.isFailure) notice = "Couldn't remove this clip. Try again." else show(next)
        }
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(SliverColors.Media)) {
                if (day != null) {
                    ClipPlayer(
                        day.file, 0, 1000, Modifier.fillMaxSize(), audio = !muted,
                    )
                }
                Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(SliverColors.Scrim, Color.Transparent))))
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SliverSpace.S3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton44(onBack, "Close", overCamera = true) { SliverIcon(SliverIcons.X, SliverColors.OnMedia) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton44({ show(prevDay(current, filled)) }, "Previous day", overCamera = true) {
                            SliverIcon(SliverIcons.ChevronLeft, SliverColors.OnMedia.copy(alpha = if (prevDay(current, filled) != null) 1f else 0.4f))
                        }
                        IconButton44({ nextDay(current, filled)?.let { show(it) } }, "Next day", overCamera = true) {
                            SliverIcon(SliverIcons.ChevronRight, SliverColors.OnMedia.copy(alpha = if (nextDay(current, filled) != null) 1f else 0.4f))
                        }
                        IconButton44({ muted = !muted }, if (muted) "Sound off" else "Sound on", overCamera = true) {
                            SliverIcon(if (muted) SliverIcons.VolumeX else SliverIcons.Volume2, if (muted) SliverColors.OnMedia.copy(alpha = 0.75f) else SliverColors.OnMedia)
                        }
                    }
                }
                if (day != null && app?.dateStamp == true) DateStamp(stampText(current))
            }
            Column(
                Modifier.fillMaxWidth().background(SliverColors.BgBase)
                    .navigationBarsPadding()
                    .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
                verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
            ) {
                SliverText(weekdayName(current), SliverType.DisplayL, color = SliverColors.TextPrimary)
                val time = day?.takenAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.time
                SliverText(
                    "Day ${dayNumber(current, first)}" + (time?.let { " · ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } ?: ""),
                    SliverType.Meta, color = SliverColors.TextSecondary,
                )
                if (day != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                        day.place?.let { Chip(it, selected = false, onClick = {}) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                    SecondaryButton("Replace", { replace(current) }, Modifier.weight(1f))
                    GhostButton("Remove", { day?.let(::remove) }, Modifier.weight(1f), destructive = true, enabled = day != null)
                }
            }
        }

        NoticeSheet(notice) { notice = null }
    }
}
