package app.sliver.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.HoldToConfirmButton
import app.sliver.core.ui.IconButton44
import app.sliver.data.backup.clipsLine
import app.sliver.data.backup.driveLine
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.SwitchRow
import app.sliver.data.BackupState
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.data.auth.AccountDeleter
import app.sliver.data.auth.AccountManager
import app.sliver.data.backup.ArchiveExporter
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.BackupProgress
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.BackupSnapshot
import app.sliver.data.backup.BackupUi
import app.sliver.data.backup.DriveClient
import app.sliver.data.backup.DriveQuota
import app.sliver.data.backup.NetworkKind
import app.sliver.data.backup.NetworkMonitor
import app.sliver.data.backup.backupUi
import app.sliver.data.backup.formatBytes
import app.sliver.data.backup.syncStatusLine
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

data class StatusCopy(val title: String, val body: String, val action: String? = null)

fun statusCopy(ui: BackupUi, progress: BackupProgress?): StatusCopy = when (ui) {
    BackupUi.BACKING_UP -> StatusCopy(
        "Syncing",
        progress?.takeIf { it.total > 0 }?.let { "${it.done} of ${it.total} clips. It picks up where it left off." }
            ?: "Getting ready. It picks up where it left off.",
    )
    BackupUi.UP_TO_DATE -> StatusCopy("All backed up", "Every clip is safe in your Drive.")
    BackupUi.PAUSED_WIFI -> StatusCopy("Waiting for Wi-Fi", "Your clips upload as soon as you're on Wi-Fi.", "Back up anyway")
    BackupUi.FAILED -> StatusCopy("Backup didn't finish", "Your clips are safe on this phone. We'll try again soon.", "Try again")
    BackupUi.DRIVE_FULL -> StatusCopy(
        "Your Drive is full",
        "Your clips are safe on this phone. Free up space in Google Drive to continue.",
        "Try again",
    )
    BackupUi.REVOKED -> StatusCopy("Access was removed", "Reconnect Google to keep backing up.", "Reconnect")
    BackupUi.OFFLINE -> StatusCopy("You're offline", "Backup resumes when you're back online.")
}

@Composable
fun BackupScreen(onBack: () -> Unit, onRestore: () -> Unit = {}) {
    val koin = remember { KoinPlatform.getKoin() }
    val repo = remember { koin.get<SettingsRepository>() }
    val manager = remember { koin.get<AccountManager>() }
    val engine = remember { koin.get<SyncEngine>() }
    val days = remember { koin.get<DayRepository>() }
    val drive = remember { koin.get<DriveClient>() }
    val scheduler = remember { koin.get<BackupScheduler>() }
    val deleter = remember { koin.get<AccountDeleter>() }
    val monitor = remember { koin.get<NetworkMonitor>() }
    val exporter = remember { koin.get<ArchiveExporter>() }
    val scope = rememberCoroutineScope()

    val settings by remember { repo.settings }.collectAsState(null)
    val statuses by remember { days.observeAll().map { list -> list.map { it.backup } } }.collectAsState(emptyList())
    val progress by engine.progress.collectAsState()
    val network by remember { monitor.network }.collectAsState(NetworkKind.UNMETERED)
    val account = settings?.account

    var quota by remember { mutableStateOf<DriveQuota?>(null) }
    var exportNote by remember { mutableStateOf<String?>(null) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteDrive by remember { mutableStateOf(false) }
    var deleteLocal by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf(false) }
    val pickZip = rememberZipSavePicker { uri ->
        scope.launch {
            exportNote = "Exporting"
            exportNote = exporter.exportAll(uri) { done, total -> exportNote = "Exporting $done of $total" }
                .fold({ "Saved. Your clips are in the .zip." }, { "Export failed. Try again." })
        }
    }

    val nowMillis = remember(settings) { kotlin.time.Clock.System.now().toEpochMilliseconds() }
    val backedUp = statuses.count { it == BackupState.DONE }
    val pending = statuses.size - backedUp
    LaunchedEffect(account?.signedIn, backedUp) {
        quota = if (account?.signedIn == true) runCatching { drive.storageQuota() }.getOrNull() else null
    }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S4),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
                SliverText("Backup & sync", SliverType.Title)
            }
            when {
                account == null -> {}
                account.signedIn -> {
                    val ui = backupUi(BackupSnapshot(pending, progress != null, account.backupError, account.wifiOnly, network))
                    val base = statusCopy(ui, progress)
                    // One status, not two: the sync line becomes the body of the "Synced" card.
                    val copy = if (ui == BackupUi.UP_TO_DATE) {
                        base.copy(title = "Synced", body = syncStatusLine(account.lastSyncAt, nowMillis, account.phones).removePrefix("Synced · ").replaceFirstChar { it.uppercase() })
                    } else base
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = SliverSpace.S6),
                        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
                    ) {
                        StatusCard(ui, copy, onAction = {
                            if (ui == BackupUi.REVOKED) scope.launch { manager.signIn() }
                            else scheduler.schedule(account.wifiOnly, anyNetwork = true)
                        })
                        InfoRow("Folder", "Fragments")
                        InfoRow("Clips", clipsLine(backedUp, statuses.size))
                        val driveText = quota?.let { driveLine(it.used, it.limit) }
                        InfoRow("Drive", driveText?.text ?: "Unavailable", warn = driveText?.low == true)
                        SwitchRow(
                            "Sync automatically", account.autoBackup,
                            { on -> scope.launch { repo.update { it.copy(account = it.account.copy(autoBackup = on)) } } },
                            caption = "New clips go up as you save them",
                        )
                        SwitchRow(
                            "Wi-Fi only", account.wifiOnly,
                            { on -> scope.launch { repo.update { it.copy(account = it.account.copy(wifiOnly = on)) } } },
                            caption = "Roughly 2 MB per clip",
                        )
                        PrimaryButton(
                            "Sync now", { scheduler.schedule(account.wifiOnly, anyNetwork = true) },
                            Modifier.fillMaxWidth(), enabled = progress == null,
                        )
                        // Restore is the first sync now, so there is no separate "Restore from Drive" button.
                        SecondaryButton("Export everything (.zip)", { pickZip("fragments-archive.zip") }, Modifier.fillMaxWidth())
                        exportNote?.let { SliverText(it, SliverType.BodySmall) }
                        SliverText("Account", SliverType.Caption, Modifier.padding(top = SliverSpace.S4))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SliverText(account.email.orEmpty(), SliverType.Body, Modifier.weight(1f), maxLines = 1)
                            GhostButton("Sign out", { confirmDisconnect = true })
                        }
                        SliverText("We keep only your email. Your videos stay in your own Drive.", SliverType.Caption)
                        GhostButton("Delete account", { showDelete = true; deleteError = false }, destructive = true)
                    }
                }
                else -> GoogleSignInPanel(skipLabel = "Not now", onFinished = onBack, onRestore = onRestore)
            }
        }
        SliverBottomSheet(confirmDisconnect, { confirmDisconnect = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText("Sign out?", SliverType.Title)
                SliverText("Backup pauses. Nothing is deleted, here or in your Drive.", SliverType.BodySmall)
                PrimaryButton(
                    "Sign out",
                    { confirmDisconnect = false; scope.launch { manager.signOut() } },
                    Modifier.fillMaxWidth(),
                )
                GhostButton("Cancel", { confirmDisconnect = false }, Modifier.fillMaxWidth())
            }
        }
        SliverBottomSheet(showDelete, { if (!deleting) showDelete = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText("Delete account?", SliverType.Title)
                SliverText(
                    "We revoke Fragments' access to your Google account and carry on as a guest.",
                    SliverType.BodySmall,
                )
                SwitchRow(
                    "Delete the Fragments folder in Drive", deleteDrive, { deleteDrive = it },
                    caption = "Your backed-up clips are removed from Drive",
                )
                SwitchRow(
                    "Delete my clips on this phone", deleteLocal, { deleteLocal = it },
                    caption = "This can't be undone",
                )
                if (deleteError) SliverText("Something went wrong. Nothing more was changed. Try again.", SliverType.Caption)
                HoldToConfirmButton(
                    if (deleting) "Deleting" else "Hold to delete account",
                    {
                        deleting = true
                        deleteError = false
                        scope.launch {
                            deleter.delete(deleteDrive, deleteLocal).fold(
                                { showDelete = false; deleteDrive = false; deleteLocal = false },
                                { deleteError = true },
                            )
                            deleting = false
                        }
                    },
                    Modifier.fillMaxWidth(), enabled = !deleting,
                )
                GhostButton("Cancel", { showDelete = false }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatusCard(ui: BackupUi, copy: StatusCopy, onAction: () -> Unit) {
    val dot: Color = when (ui) {
        BackupUi.UP_TO_DATE -> SliverColors.Success
        BackupUi.BACKING_UP -> SliverColors.AccentText
        BackupUi.PAUSED_WIFI, BackupUi.OFFLINE -> SliverColors.Warning
        BackupUi.FAILED, BackupUi.DRIVE_FULL, BackupUi.REVOKED -> SliverColors.Error
    }
    Column(
        Modifier.fillMaxWidth().background(SliverColors.Surface1, SliverShapes.M)
            .border(1.dp, SliverColors.LineHairline, SliverShapes.M).padding(SliverSpace.S4),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            SliverIcon(
                when (ui) {
                    BackupUi.UP_TO_DATE -> SliverIcons.Check
                    BackupUi.BACKING_UP -> SliverIcons.CloudUpload
                    BackupUi.PAUSED_WIFI, BackupUi.OFFLINE -> SliverIcons.Pause
                    BackupUi.FAILED, BackupUi.DRIVE_FULL, BackupUi.REVOKED -> SliverIcons.X
                },
                dot, size = 20.dp,
            )
            SliverText(copy.title, SliverType.Headline)
        }
        SliverText(copy.body, SliverType.BodySmall)
        copy.action?.let { SecondaryButton(it, onAction) }
    }
}

@Composable
private fun InfoRow(label: String, value: String, warn: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = SliverSpace.S1),
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SliverText(label, SliverType.BodySmall, Modifier.weight(1f))
        if (warn) SliverIcon(SliverIcons.AlertTriangle, SliverColors.Warning, size = 16.dp)
        SliverText(value, SliverType.Body, color = if (warn) SliverColors.Warning else SliverColors.TextPrimary)
    }
}
