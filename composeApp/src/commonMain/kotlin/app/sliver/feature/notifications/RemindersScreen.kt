package app.sliver.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sliver.core.APP_NAME
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.kit.ReelogButton
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SliverSwitch
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.LastCallTime
import app.sliver.data.settings.ReminderSettings
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.settings.rememberSettingsPlatform
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.koin.mp.KoinPlatform

private val DayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
private val DayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val Presets = listOf(
    LocalTime(7, 30) to "Morning",
    LocalTime(12, 30) to "Lunch",
    LocalTime(17, 30) to "After work",
    LocalTime(19, 0) to "Evening",
    LocalTime(20, 0) to "Default",
    LocalTime(21, 30) to "Late",
)

fun LocalTime.label() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun daysNote(days: List<Boolean>): String {
    val n = days.count { it }
    val weekdays = days.take(5).all { it } && !days[5] && !days[6]
    val weekend = days.take(5).none { it } && days[5] && days[6]
    return when {
        n == 7 -> "Every day"
        n == 0 -> "No days picked, so no reminders"
        weekdays -> "Weekdays only"
        weekend -> "Weekends only"
        else -> "$n days a week"
    }
}

fun reminderSummary(r: ReminderSettings): String {
    if (!r.enabled) return "Off"
    val days = daysNote(r.days).lowercase().replaceFirstChar { it.lowercase() }
    return "${if (r.followGoldenHour) "Golden hour" else r.time.label()}, $days"
}

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val repo = remember { koin.get<SettingsRepository>() }
    val projectRepo = remember { koin.get<ProjectRepository>() }
    val golden = remember { koin.get<GoldenHourProvider>() }
    val scope = rememberCoroutineScope()
    val platform = rememberSettingsPlatform()
    val controller = rememberPermissionController()
    val settings by remember { repo.settings }.collectAsStateWithLifecycle(null)
    val projects by remember { projectRepo.projects }.collectAsStateWithLifecycle(emptyList())
    var timeSheet by rememberSaveable { mutableStateOf(false) }
    var projectsSheet by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return Column(Modifier.fillMaxSize().background(SliverColors.BgBase)) {}
    val r = s.reminder
    val status = controller.status(PermissionKind.Notifications)
    val permOff = r.enabled && !platform.notificationsEnabled()

    fun update(transform: (ReminderSettings) -> ReminderSettings) {
        scope.launch { repo.update { it.copy(reminder = transform(it.reminder)) } }
    }

    androidx.compose.runtime.LaunchedEffect(controller.status(PermissionKind.CoarseLocation)) { golden.refresh() }

    fun turnOn(on: Boolean) {
        update { it.copy(enabled = on) }
        if (on && status == PermissionStatus.NotAsked) controller.request(PermissionKind.Notifications)
    }

    val today = todayLocal()
    val todaySun = if (r.followGoldenHour) golden.eveningGoldenHour(today) else null
    val shownTime = r.timeOn(today, golden::eveningGoldenHour)
    val open = projects.filter { !it.archived }
    val watched = if (r.projects.isEmpty()) open else open.filter { it.id in r.projects }
    val projectsValue = when {
        open.isEmpty() -> ""
        watched.size == open.size && open.size > 1 -> "All open"
        else -> watched.firstOrNull()?.name.orEmpty() + if (watched.size > 1) " +${watched.size - 1}" else ""
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = SliverSpace.Gutter).padding(bottom = SliverSpace.S8),
        ) {
            Row(Modifier.padding(top = SliverSpace.S2), verticalAlignment = Alignment.CenterVertically) {
                IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
                SliverText("Daily reminder", SliverType.DisplayM.copy(fontSize = ReelogType.Size.title), Modifier.padding(start = SliverSpace.S1))
            }

            if (permOff) PermOffRow(platform::openNotificationSettings, Modifier.padding(top = SliverSpace.S4))

            Spacer(SliverSpace.S3)
            SwitchLine("Remind me to film", "One quiet notification. Never more than two a day.", r.enabled, ::turnOn, 60.dp)

            Column(Modifier.alpha(if (r.enabled) 1f else 0.35f)) {
                Label("Remind me at", Modifier.padding(top = 20.dp, bottom = 6.dp))
                Row(
                    Modifier.clickable(role = Role.Button) { timeSheet = true }
                        .semantics { contentDescription = "Reminder time, ${shownTime.label()}. Change" },
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SliverText(
                        (if (todaySun != null) "≈" else "") + shownTime.label(),
                        ReelogType.clock,
                    )
                    SliverText(
                        "Change",
                        SliverType.Body.copy(fontSize = ReelogType.Size.label, fontWeight = FontWeight.Medium),
                        Modifier.padding(bottom = 8.dp),
                        color = SliverColors.AccentText
                    )
                }
                if (r.followGoldenHour) {
                    SliverText(
                        if (todaySun != null) {
                            "Today: ${shownTime.label()}, $GOLDEN_LEAD_MINUTES min before golden hour. Moves with the sun."
                        } else {
                            "Location isn't available, so the fixed time is used."
                        },
                        SliverType.Caption,
                        Modifier.padding(top = 6.dp),
                        color = SliverColors.AccentText,
                    )
                }

                Label("On these days", Modifier.padding(top = 22.dp, bottom = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayLetters.forEachIndexed { i, letter ->
                        DayDot(letter, DayNames[i], r.days[i]) {
                            update { cur -> cur.copy(days = cur.days.mapIndexed { j, on -> if (j == i) !on else on }) }
                        }
                    }
                }
                SliverText(daysNote(r.days), SliverType.Caption, Modifier.padding(top = 8.dp), color = SliverColors.TextTertiary)

                Label("Be smart about it", Modifier.padding(top = 24.dp, bottom = 2.dp))
                SwitchLine("Skip days that are done", "No reminder if today already has a clip.", r.skipIfDone, { on -> update {
                    it.copy(skipIfDone = on)
                } })
                SwitchLine(
                    "One last call at ${LastCallTime.label()}",
                    "A second, quieter reminder if the day is still empty.",
                    r.lastCall,
                    { on -> update { it.copy(lastCall = on) } },
                )
                SwitchLine(
                    "Follow golden hour",
                    "Remind me $GOLDEN_LEAD_MINUTES min before the evening light, instead of a fixed time. Uses approximate location.",
                    r.followGoldenHour,
                    { on ->
                        update { it.copy(followGoldenHour = on) }
                        if (on && controller.status(PermissionKind.CoarseLocation) == PermissionStatus.NotAsked) {
                            controller.request(
                                PermissionKind.CoarseLocation
                            )
                        }
                    },
                )

                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button) { projectsSheet = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SliverText("Projects", SliverType.Headline.copy(fontSize = ReelogType.Size.label))
                        SliverText("Only the ones still open", SliverType.Caption)
                    }
                    SliverText(projectsValue, SliverType.BodySmall.copy(fontSize = ReelogType.Size.label), maxLines = 1, color = SliverColors.TextTertiary)
                    SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary, size = 16.dp)
                }
                Hairline()

                Label("What it will look like", Modifier.padding(top = 24.dp, bottom = 10.dp))
                NotifPreview(shownTime.label(), reminderCopy(NotifKind.REMINDER, today).body)
                SliverText(
                    "The picture is yesterday's clip. \"Tomorrow\" just dismisses it, nothing is counted.",
                    SliverType.Caption.copy(fontSize = ReelogType.Size.caption),
                    Modifier.padding(top = 8.dp),
                    color = SliverColors.TextTertiary,
                )
            }
        }

        // Audit v1.8 §7 time picker (board phone 3): wheels, presets, "Set 15:55" — no system dialog.
        SliverBottomSheet(timeSheet, { timeSheet = false }) {
            TimeWheelSheet(
                r.time,
                r.followGoldenHour,
                platform.is24Hour,
                onGolden = {
                    update { it.copy(followGoldenHour = true) };
                    timeSheet = false
                },
                onSet = { t ->
                    update { it.copy(time = t, followGoldenHour = false) };
                    timeSheet = false
                },
            )
        }
        ProjectsPicker(projectsSheet, { projectsSheet = false }, open, watched.map { it.id }.toSet()) { picked ->
            update { it.copy(projects = if (picked.size == open.size) emptySet() else picked) }
        }
    }
}

@Composable
private fun Spacer(h: androidx.compose.ui.unit.Dp) = Box(Modifier.height(h))

@Composable
private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    SliverText(
        text,
        SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)),
        modifier,
        color = SliverColors.TextTertiary
    )
}

@Composable
private fun SwitchLine(title: String, caption: String, checked: Boolean, onChange: (Boolean) -> Unit, minHeight: androidx.compose.ui.unit.Dp = 66.dp) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = minHeight)
            .semantics {
                toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off
            }
            .clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SliverText(title, SliverType.Headline.copy(fontSize = ReelogType.Size.label))
            SliverText(caption, SliverType.Caption)
        }
        SliverSwitch(checked)
    }
    Hairline()
}

@Composable
private fun DayDot(letter: String, name: String, on: Boolean, onClick: () -> Unit) {
    app.sliver.core.kit.DayToggle(letter, on, onClick, description = name)
}

@Composable
private fun PresetTile(t: LocalTime, name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(64.dp).clip(SliverShapes.R14)
            .background(if (selected) SliverColors.AccentTint else SliverColors.Surface1)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) SliverColors.Accent else SliverColors.LineHairline, SliverShapes.R14)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliverText(t.label(), SliverType.Headline.copy(fontSize = ReelogType.Size.body, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"))
        SliverText(name, SliverType.Caption.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption)))
    }
}

@Composable
private fun PermOffRow(onTurnOn: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(SliverShapes.M).background(SliverColors.PermBannerBg)
            .border(1.dp, SliverColors.PermBannerBorder, SliverShapes.M).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SliverIcon(SliverIcons.Bell, SliverColors.PermBannerIcon, size = 18.dp)
        SliverText(
            "Notifications are off for $APP_NAME in Android settings.",
            SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)),
            Modifier.weight(1f),
            color = SliverColors.PermBannerText,
        )
        ReelogButton("Turn on", onTurnOn, small = true, fullWidth = false)
    }
}

@Composable
private fun NotifPreview(time: String, body: String) {
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
            .background(if (SliverColors.light) SliverColors.Surface1 else SliverColors.NotifPreviewDark)
            .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(24.dp).clip(SliverShapes.Pill).background(SliverColors.BgVoid), Alignment.Center) {
            Box(Modifier.size(2.dp, 13.dp).clip(SliverShapes.Pill).background(SliverColors.AccentText))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SliverText(
                "$APP_NAME · $time",
                SliverType.Caption.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption))
            )
            SliverText(
                "Today's clip",
                SliverType.Headline.copy(
                    fontSize = ReelogType.Size.label,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = ReelogType.lineFor(ReelogType.Size.label)
                )
            )
            SliverText(body, SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SliverText(
                    "Record",
                    SliverType.Headline.copy(
                        fontSize = ReelogType.Size.label,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = ReelogType.lineFor(ReelogType.Size.label)
                    ),
                    color = SliverColors.AccentText
                )
                SliverText(
                    "Tomorrow",
                    SliverType.Headline.copy(
                        fontSize = ReelogType.Size.label,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = ReelogType.lineFor(ReelogType.Size.label)
                    ),
                    color = SliverColors.TextSecondary
                )
            }
        }
        Box(
            Modifier.size(
                52.dp
            ).clip(SliverShapes.S).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(SliverColors.NotifThumbFrom, SliverColors.NotifThumbTo)))
        )
    }
}

@Composable
private fun ProjectsPicker(
    visible: Boolean,
    onDismiss: () -> Unit,
    open: List<app.sliver.data.db.Project>,
    picked: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    SliverBottomSheet(visible, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S1)) {
            SliverText("Which projects count?", SliverType.Title.copy(fontSize = ReelogType.Size.headline), Modifier.padding(bottom = SliverSpace.S2))
            open.forEach { p ->
                val on = p.id in picked
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics { selected = on }
                        .clickable(role = Role.Checkbox) {
                            val next = if (on) picked - p.id else picked + p.id
                            if (next.isNotEmpty()) onChange(next)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SliverText(p.name, SliverType.Body, Modifier.weight(1f), maxLines = 1)
                    if (on) SliverIcon(SliverIcons.Check, SliverColors.AccentText)
                }
            }
        }
    }
}

@Composable
private fun TimeWheelSheet(initial: LocalTime, golden: Boolean, is24Hour: Boolean, onGolden: () -> Unit, onSet: (LocalTime) -> Unit) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    // Minutes move in 5-min steps; a long press on the minutes switches to 1-min steps.
    var fine by remember { mutableStateOf(initial.minute % 5 != 0) }
    val step = if (fine) 1 else 5
    val minutes = (0 until 60 step step).toList()
    val hours = if (is24Hour) (0..23).toList() else (1..12).toList()
    val pm = hour >= 12
    fun shownHour(h: Int) = if (is24Hour) h else (h % 12).let { if (it == 0) 12 else it }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SliverText("Remind me at", SliverType.Title.copy(fontSize = ReelogType.Size.headline))
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.sliver.core.ui.Wheel(
                hours.map { it.toString().padStart(2, '0') },
                hours.indexOf(shownHour(hour)).coerceAtLeast(0),
                { i ->
                    val h = hours[i];
                    hour = if (is24Hour) h else (h % 12) + if (pm) 12 else 0
                },
                "Hour",
                Modifier.weight(1f),
            )
            SliverText(":", ReelogType.display, Modifier.padding(horizontal = 8.dp))
            app.sliver.core.ui.Wheel(
                minutes.map { it.toString().padStart(2, '0') },
                minutes.indexOf(minute - minute % step).coerceAtLeast(0),
                { i -> minute = minutes[i] },
                "Minute",
                Modifier.weight(1f),
                onLongPress = { fine = !fine },
            )
            if (!is24Hour) {
                app.sliver.core.ui.Wheel(
                    listOf("AM", "PM"),
                    if (pm) 1 else 0,
                    { i -> hour = hour % 12 + if (i == 1) 12 else 0 },
                    "AM or PM",
                    Modifier.weight(0.8f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.sliver.core.kit.ReelogChip("Golden hour", golden, onGolden, leading = if (golden) SliverIcons.Check else null)
            listOf(LocalTime(8, 0), LocalTime(20, 0)).forEach { t ->
                app.sliver.core.kit.ReelogChip(t.label(), !golden && hour == t.hour && minute == t.minute, {
                    hour = t.hour;
                    minute = t.minute
                })
            }
        }
        val t = LocalTime(hour, minute)
        app.sliver.core.ui.PrimaryButton(
            "Set ${if (is24Hour) {
                "${hour.toString().padStart(
                    2,
                    '0'
                )}:${minute.toString().padStart(2, '0')}"
            } else {
                "${shownHour(hour)}:${minute.toString().padStart(2, '0')} ${if (pm) "PM" else "AM"}"
            }}",
            { onSet(t) },
            Modifier.fillMaxWidth()
        )
    }
}
