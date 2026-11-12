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
import androidx.compose.runtime.mutableStateOf
import app.sliver.core.ui.SliverTextField
import app.sliver.feature.projects.cleanProjectName
import app.sliver.feature.projects.MAX_PROJECT_NAME
import app.sliver.feature.projects.FormatCards
import app.sliver.data.repo.ProjectRepository
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
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
import app.sliver.core.time.todayLocal
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
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.backup.GoogleSignInPanel
import app.sliver.feature.notifications.NotifKind
import app.sliver.feature.notifications.label
import app.sliver.feature.notifications.reminderCopy
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onRestore: () -> Unit = {}) {
    val koin = remember { KoinPlatform.getKoin() }
    val repo = remember { koin.get<SettingsRepository>() }
    val projects = remember { koin.get<ProjectRepository>() }
    val scope = rememberCoroutineScope()
    val controller = rememberPermissionController()
    val steps = remember { OnboardingFlow.steps() }
    var index by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("My ${todayLocal().year}") }
    var format by remember { mutableStateOf(FilmFormat.CINEMA) }
    var busy by remember { mutableStateOf(false) }
    val step = steps[index]
    LaunchedEffect(Unit) { FirstClipTimer.start() }

    fun finish() {
        if (busy) return
        busy = true
        scope.launch {
            repo.update { it.copy(onboardingDone = true) }
            onFinished()
        }
    }

    fun createProject() {
        val clean = cleanProjectName(name) ?: return
        if (busy) return
        busy = true
        scope.launch {
            val id = runCatching { projects.activeId() }.getOrNull()
            if (id == null) {
                projects.create(clean, format, todayLocal())
            } else {
                projects.rename(id, clean)
                projects.setFormat(id, format)
            }
            repo.update { it.copy(newFilmFormat = format) }
            busy = false
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
            Box(Modifier.width(72.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                OnboardingStep.Welcome -> Column(
                    Modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
                    verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(12.dp).height(120.dp).clip(SliverShapes.Pill).background(SliverColors.TextPrimary))
                    SliverText("One clip a day. One film a year.", SliverType.DisplayM)
                    SliverText(
                        "Catch a moment, keep the best part. Fragments turns your clips into a film.",
                        SliverType.Body, color = SliverColors.TextSecondary,
                    )
                    PrimaryButton("Start", { index++ }, Modifier.fillMaxWidth().padding(top = SliverSpace.S6))
                    GhostButton("Restore from backup", onRestore, Modifier.fillMaxWidth())
                }
                OnboardingStep.Project -> Column(
                    Modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
                    verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
                ) {
                    SliverText("Name your first project", SliverType.DisplayM)
                    SliverTextField(name, { name = it.take(MAX_PROJECT_NAME) }, placeholder = "Project name")
                    FormatCards(format) { format = it }
                    PrimaryButton(
                        "Create project", ::createProject, Modifier.fillMaxWidth(),
                        enabled = cleanProjectName(name) != null && !busy,
                    )
                }
                OnboardingStep.Camera -> CameraStep(controller, ::finish)
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
                    .background(if (i <= index) SliverColors.TextPrimary else SliverColors.LineStrong),
            )
        }
    }
}

@Composable
private fun CameraStep(controller: app.sliver.core.permissions.PermissionController, onDone: () -> Unit) {
    val status = controller.status(PermissionKind.CameraMic)
    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(status, asked) { if (asked && status != PermissionStatus.NotAsked) onDone() }
    Column(
        Modifier.fillMaxSize().padding(bottom = SliverSpace.S6),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
    ) {
        SliverText("Camera, please.", SliverType.DisplayM)
        SliverText(
            "Fragments only records while you tap or hold. Nothing leaves your phone.",
            SliverType.Body, color = SliverColors.TextSecondary,
        )
        PrimaryButton(
            "Allow camera", {
                if (status.isUsable) onDone() else { asked = true; controller.request(PermissionKind.CameraMic) }
            },
            Modifier.fillMaxWidth().padding(top = SliverSpace.S6),
        )
        SliverText("Photos and reminders are asked when you need them.", SliverType.Caption, color = SliverColors.TextTertiary)
    }
}
