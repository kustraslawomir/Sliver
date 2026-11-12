package app.sliver.feature.notifications

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SwitchRow
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.mp.KoinPlatform

internal val FixedTimes = listOf(LocalTime(9, 0), LocalTime(13, 0), LocalTime(19, 30), LocalTime(21, 30))
private val WindowStarts = listOf(LocalTime(8, 0), LocalTime(10, 0), LocalTime(12, 0))
private val WindowEnds = listOf(LocalTime(18, 0), LocalTime(20, 0), LocalTime(22, 0))
private val DayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
private val Modes = listOf(ReminderMode.FIXED, ReminderMode.RANDOM, ReminderMode.OFF)

internal fun LocalTime.label() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val repo = remember { KoinPlatform.getKoin().get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val settings by remember { repo.settings }.collectAsState(null)
    val controller = rememberPermissionController()
    val s = settings ?: return Column(Modifier.fillMaxSize().background(SliverColors.BgBase)) {}
    val status = controller.status(PermissionKind.Notifications)

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { repo.update(transform) }
    }

    fun ensurePermission() {
        if (status == PermissionStatus.NotAsked) controller.request(PermissionKind.Notifications)
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(horizontal = SliverSpace.Gutter).padding(bottom = SliverSpace.S8),
    ) {
        Row(Modifier.padding(top = SliverSpace.S2), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Notifications", SliverType.DisplayM, Modifier.padding(start = SliverSpace.S2))
        }

        if (status == PermissionStatus.Denied) {
            DeniedBanner(onOpen = controller::openSettings, Modifier.padding(top = SliverSpace.S4))
        }

        SliverText("Daily reminder", SliverType.Headline, Modifier.padding(top = SliverSpace.S6, bottom = SliverSpace.S3))
        Segmented(
            listOf("Fixed", "Random", "Off"),
            Modes.indexOf(s.reminder.mode),
            { i ->
                update { it.copy(reminder = it.reminder.copy(mode = Modes[i])) }
                if (Modes[i] != ReminderMode.OFF) ensurePermission()
            },
        )

        when (s.reminder.mode) {
            ReminderMode.FIXED -> {
                Label("Time")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    FixedTimes.forEach { t ->
                        Chip(t.label(), s.reminder.time == t, { update { it.copy(reminder = it.reminder.copy(time = t)) } })
                    }
                }
            }
            ReminderMode.RANDOM -> {
                Label("Window starts")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    WindowStarts.forEach { t ->
                        Chip(t.label(), s.reminder.windowStart == t, { update { it.copy(reminder = it.reminder.copy(windowStart = t)) } })
                    }
                }
                Label("Window ends")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    WindowEnds.forEach { t ->
                        Chip(t.label(), s.reminder.windowEnd == t, { update { it.copy(reminder = it.reminder.copy(windowEnd = t)) } })
                    }
                }
            }
            ReminderMode.OFF -> Unit
        }

        if (s.reminder.mode != ReminderMode.OFF) {
            Label("Days")
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                DayLetters.forEachIndexed { i, letter ->
                    Chip(letter, s.reminder.days[i], {
                        update { cur ->
                            cur.copy(reminder = cur.reminder.copy(days = cur.reminder.days.mapIndexed { j, on -> if (j == i) !on else on }))
                        }
                    })
                }
            }
            NotifPreview(
                reminderCopy(if (s.reminder.mode == ReminderMode.RANDOM) NotifKind.RANDOM else NotifKind.REMINDER, LocalDate(2000, 1, 1)),
                Modifier.padding(top = SliverSpace.S5),
            )
        }

        SliverText("Also tell me", SliverType.Headline, Modifier.padding(top = SliverSpace.S7, bottom = SliverSpace.S2))
        SwitchRow("Running late", s.notif.runningLate, { on ->
            update { it.copy(notif = it.notif.copy(runningLate = on)) }
            if (on) ensurePermission()
        }, caption = "At 21:00 if there's no clip yet")
        SwitchRow("Film ready", s.notif.filmReady, { on -> update { it.copy(notif = it.notif.copy(filmReady = on)) } })
        SwitchRow("Backup problems", s.notif.backupProblems, { on -> update { it.copy(notif = it.notif.copy(backupProblems = on)) } })
        SwitchRow("On this day", s.notif.onThisDay, { on -> update { it.copy(notif = it.notif.copy(onThisDay = on)) } })
    }
}

@Composable
private fun Label(text: String) {
    SliverText(text, SliverType.Caption, Modifier.padding(top = SliverSpace.S5, bottom = SliverSpace.S2), color = SliverColors.TextTertiary)
}

@Composable
internal fun NotifPreview(copy: NotifCopy, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(SliverShapes.M).background(SliverColors.Surface2)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.M).padding(SliverSpace.S4),
    ) {
        SliverText("Sliver  ·  now", SliverType.Meta, color = SliverColors.TextTertiary)
        SliverText(copy.title, SliverType.Headline, Modifier.padding(top = SliverSpace.S1))
        SliverText(copy.body, SliverType.BodySmall, color = SliverColors.TextSecondary)
    }
}

@Composable
private fun DeniedBanner(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(SliverShapes.M).background(SliverColors.Surface1)
            .border(1.dp, SliverColors.LineStrong, SliverShapes.M).padding(SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        SliverText("Notifications are off", SliverType.Headline)
        SliverText(
            "Reminders can't reach you right now. Your clips are unaffected, and you can turn this on whenever you like.",
            SliverType.BodySmall, color = SliverColors.TextSecondary,
        )
        PrimaryButton("Open Settings", onOpen)
    }
}
