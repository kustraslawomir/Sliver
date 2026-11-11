package app.sliver.data.backup

import app.sliver.data.BackupState
import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface BackupScheduler {
    /** Queues one unique backup run. [anyNetwork] ignores the Wi-Fi-only preference (manual "Back up now"). */
    fun schedule(wifiOnly: Boolean, anyNetwork: Boolean = false)
    fun cancel()
}

interface NetworkMonitor {
    val network: Flow<NetworkKind>
}

interface ArchiveExporter {
    /** Writes every day video plus manifest.json as a .zip to [destination] (a SAF document uri). */
    suspend fun exportAll(destination: String, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Result<Unit>
}

/** Queues a backup whenever something is waiting and auto-backup is on. */
class BackupCoordinator(
    private val days: DayRepository,
    private val settings: SettingsRepository,
    private val scheduler: BackupScheduler,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                settings.settings.map { (it.account.signedIn && it.account.autoBackup) to it.account.wifiOnly },
                days.observeAll().map { list -> list.any { it.backup == BackupState.PENDING } },
            ) { (active, wifiOnly), pending -> Triple(active, wifiOnly, pending) }
                .distinctUntilChanged()
                .collect { (active, wifiOnly, pending) ->
                    if (!active) scheduler.cancel() else if (pending) scheduler.schedule(wifiOnly)
                }
        }
    }
}
