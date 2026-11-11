package app.sliver.feature.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SliverTextField
import app.sliver.core.ui.rememberSingleFlight
import app.sliver.core.ui.rememberTapGate
import app.sliver.data.FilmFormat
import app.sliver.data.db.Project
import app.sliver.data.db.ProjectStats
import app.sliver.data.repo.ProjectRepository
import app.sliver.feature.camera.label
import app.sliver.feature.trim.rememberPhoto
import org.koin.mp.KoinPlatform

private sealed interface SheetMode {
    data object List : SheetMode
    data object New : SheetMode
    data class Menu(val project: Project) : SheetMode
    data class Rename(val project: Project) : SheetMode
    data class Format(val project: Project) : SheetMode
    data class Delete(val project: Project) : SheetMode
}

/** Direct entry into one of the per-project forms, used by the Project screen menu. */
enum class ProjectAction { Rename, Format, Delete }

/** Projects list, new-project form and the per-project menu, all in one bottom sheet. */
@Composable
fun ProjectsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    initialMenuFor: String? = null,
    action: ProjectAction? = null,
) {
    val repo = remember { KoinPlatform.getKoin().get<ProjectRepository>() }
    val projects by remember { repo.projects }.collectAsState(emptyList())
    val stats by remember { repo.stats }.collectAsState(emptyMap())
    val active by remember { repo.active }.collectAsState(null)
    val scope = rememberCoroutineScope()
    val flight = rememberSingleFlight()
    var mode by remember { mutableStateOf<SheetMode>(SheetMode.List) }

    LaunchedEffect(visible, initialMenuFor, action, projects.isEmpty(), active?.id) {
        mode = if (visible) {
            val target = active.takeIf { action != null }
            when {
                target != null -> when (action) {
                    ProjectAction.Rename -> SheetMode.Rename(target)
                    ProjectAction.Format -> SheetMode.Format(target)
                    else -> SheetMode.Delete(target)
                }
                else -> projects.firstOrNull { it.id == initialMenuFor }?.let { SheetMode.Menu(it) } ?: SheetMode.List
            }
        } else SheetMode.List
    }
    fun backToList() { if (action != null) onDismiss() else mode = SheetMode.List }
    fun backToMenu(p: Project) { if (action != null) onDismiss() else mode = SheetMode.Menu(p) }

    fun run(block: suspend () -> Unit) = flight.launch(scope) {
        block()
        backToList()
    }

    SliverBottomSheet(visible, onDismiss) {
        when (val m = mode) {
            SheetMode.List -> ProjectList(
                projects, stats, active?.id,
                onPick = { p ->
                    flight.launch(scope) {
                        if (p.archived) repo.setArchived(p.id, false)
                        repo.setActive(p.id)
                        onDismiss()
                    }
                },
                onMenu = { mode = SheetMode.Menu(it) },
                onNew = { mode = SheetMode.New },
            )
            SheetMode.New -> NewProject(
                existing = projects.map { it.name },
                busy = flight.running,
                onBack = ::backToList,
                onCreate = { name, format ->
                    flight.launch(scope) {
                        repo.create(name, format, todayLocal())
                        onDismiss()
                    }
                },
            )
            is SheetMode.Menu -> ProjectMenu(
                m.project, canLeaveActive(m.project, projects),
                onBack = ::backToList,
                onRename = { mode = SheetMode.Rename(m.project) },
                onFormat = { mode = SheetMode.Format(m.project) },
                onArchive = { run { repo.setArchived(m.project.id, !m.project.archived) } },
                onDelete = { mode = SheetMode.Delete(m.project) },
            )
            is SheetMode.Rename -> RenameProject(
                m.project, flight.running,
                onBack = { backToMenu(m.project) },
                onSave = { name -> run { repo.rename(m.project.id, name) } },
            )
            is SheetMode.Format -> FormatProject(
                m.project,
                onBack = { backToMenu(m.project) },
                onPick = { format -> run { repo.setFormat(m.project.id, format) } },
            )
            is SheetMode.Delete -> ConfirmDelete(
                m.project, stats[m.project.id]?.clips ?: 0, flight.running,
                onBack = { backToMenu(m.project) },
                onConfirm = { run { repo.delete(m.project.id) } },
            )
        }
    }
}

@Composable
private fun SheetHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
        }
        SliverText(title, SliverType.DisplayM, Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    stats: Map<String, ProjectStats>,
    activeId: String?,
    onPick: (Project) -> Unit,
    onMenu: (Project) -> Unit,
    onNew: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SheetHeader("Projects")
        Column(
            Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        ) {
            val (open, archived) = projects.partition { !it.archived }
            open.forEach { ProjectRow(it, stats[it.id], it.id == activeId, onPick, onMenu) }
            if (archived.isNotEmpty()) {
                SliverText("ARCHIVED", SliverType.MonoS, Modifier.padding(top = SliverSpace.S3), SliverColors.TextTertiary)
                archived.forEach { ProjectRow(it, stats[it.id], false, onPick, onMenu) }
            }
        }
        GhostButton("New project", onNew, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    stats: ProjectStats?,
    active: Boolean,
    onPick: (Project) -> Unit,
    onMenu: (Project) -> Unit,
) {
    val cover = stats?.cover?.let { rememberPhoto(it) }
    val pick = rememberTapGate { onPick(project) }
    Row(
        Modifier.fillMaxWidth().clip(SliverShapes.M).clickable(onClick = pick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Box(Modifier.size(56.dp).clip(SliverShapes.S).background(SliverColors.Surface3)) {
            if (cover != null) Image(cover, null, Modifier.size(56.dp), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f)) {
            SliverText(project.name, SliverType.DisplayS, maxLines = 1)
            SliverText(projectMeta(project, stats), SliverType.MonoS, color = SliverColors.TextTertiary, maxLines = 1)
        }
        if (active) SliverIcon(SliverIcons.Check, SliverColors.TextPrimary)
        IconButton44({ onMenu(project) }, "Project options") { SliverIcon(SliverIcons.More, SliverColors.TextSecondary) }
    }
}

@Composable
private fun NewProject(
    existing: List<String>,
    busy: Boolean,
    onBack: () -> Unit,
    onCreate: (String, FilmFormat) -> Unit,
) {
    var name by remember { mutableStateOf(nextProjectName(existing)) }
    var format by remember { mutableStateOf(FilmFormat.CINEMA) }
    val clean = cleanProjectName(name)
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SheetHeader("New project", onBack)
        SliverTextField(name, { name = it.take(MAX_PROJECT_NAME) }, placeholder = "Project name")
        FormatCards(format) { format = it }
        SliverText("STARTS TODAY", SliverType.MonoS, color = SliverColors.TextTertiary)
        PrimaryButton(
            if (busy) "Creating..." else "Create project",
            { clean?.let { onCreate(it, format) } },
            enabled = clean != null && !busy,
        )
    }
}

@Composable
fun FormatCards(selected: FilmFormat, onPick: (FilmFormat) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        FilmFormat.entries.forEach { f ->
            val on = f == selected
            val tap = rememberTapGate { onPick(f) }
            Column(
                Modifier.weight(1f).clip(SliverShapes.M)
                    .background(if (on) SliverColors.AccentTint else SliverColors.Surface1)
                    .border(1.dp, if (on) SliverColors.AccentLine else SliverColors.LineHairline, SliverShapes.M)
                    .clickable(onClick = tap).padding(SliverSpace.S4),
                verticalArrangement = Arrangement.spacedBy(SliverSpace.S1),
            ) {
                SliverText(f.label(), SliverType.Headline)
                SliverText(f.useLine(), SliverType.Caption)
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit, destructive: Boolean = false, enabled: Boolean = true) {
    val tap = rememberTapGate(onClick)
    SliverText(
        label, SliverType.Body,
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = enabled, onClick = tap)
            .padding(vertical = SliverSpace.S3),
        when {
            !enabled -> SliverColors.TextDisabled
            destructive -> SliverColors.Error
            else -> SliverColors.TextPrimary
        },
    )
}

@Composable
private fun ProjectMenu(
    project: Project,
    canLeave: Boolean,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onFormat: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        SheetHeader(project.name, onBack)
        MenuRow("Rename", onRename)
        MenuRow("Change format", onFormat)
        MenuRow(if (project.archived) "Unarchive" else "Archive", onArchive, enabled = canLeave || project.archived)
        MenuRow("Delete project", onDelete, destructive = true, enabled = canLeave)
        if (!canLeave) SliverText("This is your only open project.", SliverType.Caption)
    }
}

@Composable
private fun RenameProject(project: Project, busy: Boolean, onBack: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(project.name) }
    val clean = cleanProjectName(name)
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SheetHeader("Rename", onBack)
        SliverTextField(name, { name = it.take(MAX_PROJECT_NAME) }, placeholder = "Project name")
        PrimaryButton("Save", { clean?.let(onSave) }, enabled = clean != null && !busy)
    }
}

@Composable
private fun FormatProject(project: Project, onBack: () -> Unit, onPick: (FilmFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SheetHeader("Format", onBack)
        FormatCards(project.format, onPick)
        SliverText("Applies to clips you save from now on. Saved clips keep their format.", SliverType.Caption)
    }
}

@Composable
private fun ConfirmDelete(project: Project, clips: Int, busy: Boolean, onBack: () -> Unit, onConfirm: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
        SheetHeader("Delete ${project.name}?", onBack)
        SliverText(
            "This removes its $clips ${if (clips == 1) "clip" else "clips"} and films from this phone and your Drive backup.",
            SliverType.BodySmall,
        )
        PrimaryButton(if (busy) "Deleting..." else "Delete project", onConfirm, enabled = !busy)
        GhostButton("Keep it", onBack, Modifier.fillMaxWidth())
    }
}
