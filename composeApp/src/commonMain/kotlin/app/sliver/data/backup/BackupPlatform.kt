package app.sliver.data.backup

import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val SAVE_DEBOUNCE_MS = 30_000L

interface BackupScheduler {
    /** Queues one unique backup run. [anyNetwork] ignores the Wi-Fi-only preference (manual "Back up now"). */
    fun schedule(wifiOnly: Boolean, anyNetwork: Boolean = false)

    /** Keeps an hourly sync queued (needs a network) so the other phone's changes arrive without opening the app. */
    fun keepSynced(wifiOnly: Boolean)
    fun cancel()
}

interface NetworkMonitor {
    val network: Flow<NetworkKind>
}

interface ArchiveExporter {
    /** Writes every day video plus manifest.json as a .zip to [destination] (a SAF document uri). */
    suspend fun exportAll(destination: String, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Result<Unit>
}

/** Queues a sync on app start, 30 s after the last save or delete, and hourly, while signed in with auto-backup on. */
class BackupCoordinator(
    private val days: DayRepository,
    private val settings: SettingsRepository,
    private val scheduler: BackupScheduler,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            settings.settings.map { (it.account.signedIn && it.account.autoBackup) to it.account.wifiOnly }
                .distinctUntilChanged()
                .collect { (active, wifiOnly) ->
                    if (!active) scheduler.cancel() else {
                        scheduler.keepSynced(wifiOnly)
                        scheduler.schedule(wifiOnly)
                    }
                }
        }
        scope.launch {
            combine(
                settings.settings.map { (it.account.signedIn && it.account.autoBackup) to it.account.wifiOnly }.distinctUntilChanged(),
                days.observeAllProjects().map { list -> list.filter { it.updatedBy.isEmpty() }.let { own -> own.size to (own.maxOfOrNull { it.updatedAt } ?: 0L) } }
                    .distinctUntilChanged(),
            ) { account, _ -> account }
                .drop(1)
                .debounce(SAVE_DEBOUNCE_MS)
                .collect { (active, wifiOnly) -> if (active) scheduler.schedule(wifiOnly) }
        }
    }
}
