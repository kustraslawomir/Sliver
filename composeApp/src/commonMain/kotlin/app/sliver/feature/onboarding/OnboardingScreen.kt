package app.sliver.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.sliver.core.permissions.PermissionGate
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.Segmented
import app.sliver.data.FilmFormat
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.backup.GoogleSignInPanel
import app.sliver.feature.notifications.FixedTimes
import app.sliver.feature.notifications.NotifKind
import app.sliver.feature.notifications.NotifPreview
import app.sliver.feature.notifications.label
import app.sliver.feature.notifications.reminderCopy
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onRestore: () -> Unit = {}) {
    val repo = remember { KoinPlatform.getKoin().get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val settings by remember { repo.settings }.collectAsState(null)
    val controller = rememberPermissionController()
    val steps = remember { OnboardingFlow.steps() }
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val s = settings ?: AppSettings()

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { repo.update(transform) }
    }

    fun next() {
        if (OnboardingFlow.isLast(index, steps)) {
            scope.launch {
                repo.update { it.copy(onboardingDone = true) }
                onFinished()
            }
        } else {
            index++
        }
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = SliverSpace.Gutter),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(72.dp)) {
                if (OnboardingFlow.canGoBack(index)) GhostButton("Back", onClick = { index-- })
            }
            SlitProgress(steps.size, index, Modifier.weight(1f))
            Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                if (OnboardingFlow.canSkip(step)) GhostButton("Skip", onClick = { index = steps.indexOf(OnboardingStep.CameraMic) })
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                OnboardingStep.Concept -> Pitch("One second. Every day.", "Capture a single second of each day. A year later, it's a film.") { PrimaryButton("Continue", ::next, Modifier.fillMaxWidth()) }
                OnboardingStep.AutoPick -> Pitch("We find the best second. You have the final say.", "SLIVER picks the moment for you. Drag the window any time to choose your own.") { PrimaryButton("Continue", ::next, Modifier.fillMaxWidth()) }
                OnboardingStep.Format -> Pitch("Watch your year add up.", "Pick the shape of your films. You can change it per film later.") {
                    Segmented(
                        listOf("CINEMA 16:9", "STORY 9:16"),
                        if (s.newFilmFormat == FilmFormat.CINEMA) 0 else 1,
                        { i -> update { it.copy(newFilmFormat = if (i == 0) FilmFormat.CINEMA else FilmFormat.STORY) } },
                    )
                    PrimaryButton("Continue", ::next, Modifier.fillMaxWidth())
                }
                OnboardingStep.CameraMic -> PermissionStep(PermissionKind.CameraMic, controller, ::next)
                OnboardingStep.Photos -> PermissionStep(PermissionKind.Photos, controller, ::next)
                OnboardingStep.Reminder -> ReminderStep(
                    s,
                    onChange = { t -> update(t) },
                    onContinue = {
                        if (s.reminder.mode != ReminderMode.OFF &&
                            controller.status(PermissionKind.Notifications) == PermissionStatus.NotAsked
                        ) controller.request(PermissionKind.Notifications)
                        next()
                    },
                    onSkip = { update { it.copy(reminder = it.reminder.copy(mode = ReminderMode.OFF)) }; next() },
                )
                OnboardingStep.Google -> GoogleSignInPanel(
                    "Skip — start as guest", onFinished = ::next,
                    onRestore = { scope.launch { repo.update { it.copy(onboardingDone = true) }; onRestore() } },
                )
            }
        }
    }
}

@Composable
private fun SlitProgress(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S1, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier.width(if (i == index) 24.dp else 8.dp).height(4.dp).clip(SliverShapes.Pill)
                    .background(if (i <= index) SliverColors.Accent else SliverColors.LineStrong),
            )
        }
    }
}

@Composable
private fun Pitch(title: String, body: String, actions: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
    ) {
        SliverText(title, SliverType.DisplayM)
        SliverText(body, SliverType.Body, color = SliverColors.TextSecondary)
        Column(Modifier.padding(top = SliverSpace.S6), verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) { actions() }
    }
}

@Composable
private fun PermissionStep(kind: PermissionKind, controller: app.sliver.core.permissions.PermissionController, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PermissionGate(kind, controller, onSkip = onNext) {
            Pitch("All set", "You can change this any time in Settings.") { PrimaryButton("Continue", onNext, Modifier.fillMaxWidth()) }
        }
        if (controller.status(kind) == PermissionStatus.Denied) {
            GhostButton("Continue without", onClick = onNext, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SliverSpace.S6))
        }
    }
}

@Composable
private fun ReminderStep(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onContinue: () -> Unit, onSkip: () -> Unit) {
    val random = s.reminder.mode == ReminderMode.RANDOM
    Column(Modifier.fillMaxSize().padding(bottom = SliverSpace.S6), verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically)) {
        SliverText("When should we nudge you?", SliverType.DisplayM)
        Segmented(
            listOf("Fixed time", "Random window"),
            if (random) 1 else 0,
            { i -> onChange { it.copy(reminder = it.reminder.copy(mode = if (i == 0) ReminderMode.FIXED else ReminderMode.RANDOM)) } },
        )
        if (random) {
            SliverText("Somewhere between ${s.reminder.windowStart.label()} and ${s.reminder.windowEnd.label()}, a little different each day.", SliverType.BodySmall)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                FixedTimes.forEach { t ->
                    Chip(t.label(), s.reminder.mode == ReminderMode.FIXED && s.reminder.time == t, {
                        onChange { it.copy(reminder = it.reminder.copy(mode = ReminderMode.FIXED, time = t)) }
                    })
                }
            }
        }
        NotifPreview(reminderCopy(if (random) NotifKind.RANDOM else NotifKind.REMINDER, LocalDate(2000, 1, 1)))
        PrimaryButton("Set reminder", onContinue, Modifier.fillMaxWidth())
        GhostButton("Not now", onClick = onSkip)
    }
}
