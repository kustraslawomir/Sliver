package app.sliver.feature.backup

import app.sliver.core.APP_NAME

import app.sliver.core.ui.SlitLoader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.PrimaryButton
import app.sliver.data.backup.BackupProgress
import app.sliver.data.backup.RestoreEngine
import app.sliver.data.backup.RestoreScan
import app.sliver.data.backup.RestoreScheduler
import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.koin.mp.KoinPlatform

enum class RestoreStage { SCANNING, FOUND, NONE, FAILED, RESTORING, DONE }

fun foundCopy(count: Int) = if (count == 1) "We found 1 clip" else "We found $count clips"

fun restoreStage(scan: RestoreScan?, failed: Boolean, started: Boolean, progress: BackupProgress?, missing: Int): RestoreStage = when {
    failed -> RestoreStage.FAILED
    scan == null -> RestoreStage.SCANNING
    scan.items.isEmpty() -> RestoreStage.NONE
    !started -> RestoreStage.FOUND
    progress != null || missing > 0 -> RestoreStage.RESTORING
    else -> RestoreStage.DONE
}

/** Cells (null = leading blank) of the 7-column month grid that contains [anchor]. */
fun monthCells(anchor: LocalDate, days: Collection<LocalDate>): List<Pair<Int, Boolean>?> {
    val first = LocalDate(anchor.year, anchor.month, 1)
    val length = (first.month.length(first.year))
    val lead = first.dayOfWeek.ordinal
    val have = days.filter { it.year == anchor.year && it.month == anchor.month }.map { it.day }.toSet()
    return List(lead) { null } + (1..length).map { it to (it in have) }
}

private fun Month.length(year: Int): Int = when (number) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

@Composable
fun RestoreScreen(onDone: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val engine = remember { koin.get<RestoreEngine>() }
    val scheduler = remember { koin.get<RestoreScheduler>() }
    val repo = remember { koin.get<DayRepository>() }

    var scan by remember { mutableStateOf<RestoreScan?>(null) }
    var failed by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    val progress by engine.progress.collectAsState()
    val local by remember { repo.observeAllProjects() }.collectAsState(emptyList())

    val settingsRepo = remember { koin.get<SettingsRepository>() }
    val signedIn = remember { settingsRepo.settings }.collectAsState(null).value?.account?.signedIn
    val scope = rememberCoroutineScope()
    if (signedIn == false) {
        // Restore needs the Google account first; after sign-in this screen continues with the Drive scan.
        Box(Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding().padding(horizontal = SliverSpace.Gutter)) {
            GoogleSignInPanel(
                skipLabel = "Not now",
                onFinished = { scope.launch { if (!settingsRepo.settings.first().account.signedIn) onDone() } },
                onRestore = {},
            )
        }
        return
    }
    LaunchedEffect(signedIn) {
        if (signedIn != true) return@LaunchedEffect
        runCatching { engine.scan() }.onSuccess { scan = it }.onFailure { failed = true }
    }
    val have = local.map { it.projectId to it.date }.toSet()
    val missing = scan?.items?.count { (it.projectId to it.date) !in have } ?: 0
    val stage = restoreStage(scan, failed, started, progress, missing)

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
    ) {
        val total = scan?.items?.size ?: 0
        when (stage) {
            RestoreStage.SCANNING -> {
                SliverText("Welcome back", SliverType.DisplayM)
                SlitLoader()
                SliverText("Looking in your Drive.", SliverType.Body, color = SliverColors.TextSecondary)
            }
            RestoreStage.FOUND -> {
                SliverText("Welcome back", SliverType.DisplayM)
                SliverText(foundCopy(total), SliverType.Headline)
                SliverText(
                    "They download in the background. You can keep using the camera. A day you capture here stays as it is.",
                    SliverType.Body, color = SliverColors.TextSecondary,
                )
                PrimaryButton("Restore all", { started = true; scheduler.start() }, Modifier.fillMaxWidth())
                GhostButton("Not now", onClick = onDone)
            }
            RestoreStage.NONE -> {
                SliverText("Nothing to restore", SliverType.DisplayM)
                SliverText("We didn't find any clips in your $APP_NAME folder.", SliverType.Body, color = SliverColors.TextSecondary)
                GhostButton("Continue", onClick = onDone)
            }
            RestoreStage.FAILED -> {
                SliverText("Couldn't reach Drive", SliverType.DisplayM)
                SliverText("Check your connection and try again.", SliverType.Body, color = SliverColors.TextSecondary)
                GhostButton("Continue", onClick = onDone)
            }
            RestoreStage.RESTORING, RestoreStage.DONE -> {
                val restoring = stage == RestoreStage.RESTORING
                SliverText(if (restoring) "Restoring" else "All back", SliverType.DisplayM)
                SliverText(
                    if (restoring) "${total - missing} of $total clips. It picks up where it left off." else "${foundCopy(total)} restored.",
                    SliverType.Body, color = SliverColors.TextSecondary,
                )
                val anchor = local.maxByOrNull { it.date }?.date
                if (anchor != null) Mosaic(anchor, local)
                PrimaryButton(if (restoring) "Open camera" else "Done", onDone, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Mosaic(anchor: LocalDate, days: List<Day>) {
    val byDay = days.filter { it.date.year == anchor.year && it.date.month == anchor.month }.associateBy { it.date.day }
    val cells = monthCells(anchor, days.map { it.date })
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val cell = week.getOrNull(i)
                    val fill: Color? = cell?.let { (d, _) -> byDay[d]?.let { if (it.color != 0) Color(it.color) else SliverColors.Accent } }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(fill ?: if (cell != null) SliverColors.Surface1 else Color.Transparent, SliverShapes.S),
                    )
                }
            }
        }
    }
}
