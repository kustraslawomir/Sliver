package app.sliver.feature.notifications

import android.content.Context
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.BackupProgress
import app.sliver.feature.autoimport.AutoImportStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Mirrors sync (restore is its first pull) and auto-import progress into notifications; each posts at most once a second. */

const val FILL_NOTIFICATION_ID = 9203

class ProgressCoordinator(
    private val context: Context,
    private val backup: SyncEngine,
    private val autoImport: AutoImportStore,
) {
    fun start(scope: CoroutineScope) {
        mirror(scope, backup.progress, ProgressNotifier(context, 9201, DeepLinkTarget.Backup), "Syncing with Drive") { "${it.done} of ${it.total} clips" }
        val fill = autoImport.run.map { r -> r?.takeIf { !it.finished }?.let { BackupProgress(it.done, it.total) } }
        mirror(scope, fill, ProgressNotifier(context, FILL_NOTIFICATION_ID, DeepLinkTarget.FillThePast), "Importing drafts") { "${it.done} of ${it.total} days" }
    }

    private fun mirror(
        scope: CoroutineScope,
        flow: Flow<BackupProgress?>,
        notifier: ProgressNotifier,
        title: String,
        text: (BackupProgress) -> String,
    ) {
        scope.launch {
            flow.conflate().collect { p ->
                if (p == null) {
                    notifier.cancel()
                } else {
                    notifier.post(title, if (p.total == 0) "Preparing" else text(p), p.done, p.total)
                    delay(1_000)
                }
            }
        }
    }
}
