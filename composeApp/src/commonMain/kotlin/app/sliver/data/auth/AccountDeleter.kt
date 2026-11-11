package app.sliver.data.auth

import app.sliver.data.backup.DriveClient
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first

class AccountDeleter(
    private val manager: AccountManager,
    private val drive: DriveClient,
    private val settings: SettingsRepository,
    private val deleteLocalSeconds: suspend () -> Unit,
) {
    /** Optional Drive and local deletion, then token revoke; the app carries on as a guest. */
    suspend fun delete(deleteDriveFolder: Boolean, deleteLocal: Boolean): Result<Unit> = runCatching {
        if (deleteDriveFolder) {
            val stored = settings.settings.first().account.driveFolderId
            drive.trash(stored ?: drive.ensureFolder())
        }
        if (deleteLocal) deleteLocalSeconds()
        manager.revoke()
    }
}
