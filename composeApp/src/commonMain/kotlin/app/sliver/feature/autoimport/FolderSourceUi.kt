package app.sliver.feature.autoimport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sliver.core.APP_NAME
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.SecondaryButton
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/** Gallery or a picked folder as the source of a fill run; the folder and its grant outlive the screen. */
class FolderChoice(
    val useFolder: Boolean,
    val setUseFolder: (Boolean) -> Unit,
    val folder: FolderSource?,
    val grantOk: Boolean,
    val pick: () -> Unit,
    val forget: () -> Unit,
) {
    val ready get() = useFolder && folder != null && grantOk
    val tree get() = folder?.tree.takeIf { useFolder }
    val name get() = folder?.name.takeIf { useFolder }
}

@Composable
fun rememberFolderChoice(): FolderChoice {
    val koin = remember { KoinPlatform.getKoin() }
    val store = remember { koin.get<FolderSourceStore>() }
    val access = remember { koin.get<FolderAccess>() }
    val scope = rememberCoroutineScope()
    var useFolder by rememberSaveable { mutableStateOf(false) }
    val folder by remember { store.folder }.collectAsStateWithLifecycle(null)
    var tick by remember { mutableStateOf(0) }
    val grantOk = remember(folder, tick) {
        folder?.let { runCatching { access.isGranted(it.tree) }.getOrDefault(false) } == true
    }
    val pick = rememberFolderPicker { tree ->
        scope.launch {
            runCatching {
                folder?.takeIf { it.tree != tree }?.let { access.release(it.tree) }
                access.persist(tree)
                store.set(FolderSource(tree, access.displayName(tree)))
                tick++
            }
        }
    }
    return FolderChoice(
        useFolder,
        { useFolder = it },
        folder,
        grantOk,
        pick,
        forget = {
            scope.launch {
                folder?.let { access.release(it.tree) }
                store.forget()
                tick++
            }
        },
    )
}

@Composable
fun FolderSourcePicker(choice: FolderChoice) {
    Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText("Source", SliverType.BodySmall, color = SliverColors.TextTertiary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            Chip("Gallery", !choice.useFolder, { choice.setUseFolder(false) })
            Chip("Folder", choice.useFolder, { choice.setUseFolder(true) })
        }
    }
    if (choice.useFolder) FolderCard(choice)
}

@Composable
private fun FolderCard(choice: FolderChoice) {
    val folder = choice.folder
    Column(
        Modifier.fillMaxWidth().clip(
            SliverShapes.R12
        ).background(SliverColors.Surface1).border(1.dp, SliverColors.LineStrong, SliverShapes.R12).padding(SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        if (folder == null) {
            SliverText("No folder chosen", SliverType.Headline, color = SliverColors.TextPrimary)
            SliverText("$APP_NAME only reads it. Nothing in the folder is changed or deleted.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            SecondaryButton("Choose a folder...", choice.pick, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverIcon(if (choice.grantOk) SliverIcons.Folder else SliverIcons.AlertTriangle, SliverColors.TextPrimary)
                SliverText(folder.name.ifEmpty {
                    "Folder"
                }, SliverType.Headline, Modifier.weight(1f), color = SliverColors.TextPrimary, maxLines = 1)
            }
            if (!choice.grantOk) {
                SliverText("$APP_NAME lost access to this folder. Choose it again to continue.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                SecondaryButton(if (choice.grantOk) "Change" else "Choose again", choice.pick, fullWidth = false)
                GhostButton("Forget", choice.forget)
            }
        }
    }
}

/** Folder-only lines of the run screen: what could not be read, which dates are only file times, empty results. */
@Composable
fun FolderRunNotes(run: ImportRun) {
    if (run.folder == null || run.scanning) return
    Column(Modifier.padding(top = SliverSpace.S2), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        val empty = when {
            !run.finished -> null
            run.scanned == 0 -> "No photos or videos with a usable date were found in this folder."
            run.total == 0 -> "None of the ${groupThousands(run.scanned)} items belong to an empty day since the start date."
            else -> null
        }
        empty?.let { SliverText(it, SliverType.BodySmall, color = SliverColors.TextSecondary) }
        folderSummary(run.unreadable, run.fileTimeDates, run.truncated).forEach {
            SliverText(it, SliverType.BodySmall, color = SliverColors.TextSecondary)
        }
    }
}
