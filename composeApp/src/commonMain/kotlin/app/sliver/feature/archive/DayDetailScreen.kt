package app.sliver.feature.archive

import app.sliver.feature.gallery.rememberAddMedia
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import app.sliver.core.designsystem.Glow
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
import app.sliver.core.ui.NoticeSheet
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.LookParams
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
    var previewLook by remember(current) { mutableStateOf<LookParams?>(null) }

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

    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
        if (day != null) {
            ClipPlayer(
                day.file, 0, 1000, Modifier.fillMaxSize(), audio = !muted, look = previewLook ?: LookParams(),
            )
            if (app?.dateStamp == true) {
                SliverText(
                    "${current.day.toString().padStart(2, '0')}.${current.month.ordinal.plus(1).toString().padStart(2, '0')}.${current.year}",
                    SliverType.Stamp.copy(shadow = Shadow(SliverColors.Accent.copy(alpha = Glow.stampShadowAlpha), blurRadius = 6f * LocalDensity.current.density)),
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
                    IconButton44({ muted = !muted }, if (muted) "Sound off" else "Sound on") {
                        SliverIcon(if (muted) SliverIcons.VolumeX else SliverIcons.Volume2, if (muted) SliverColors.TextTertiary else SliverColors.AccentText)
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
                    "Day ${dayNumber(current, first)}" + (time?.let { " · ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } ?: ""),
                    SliverType.Meta,
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
