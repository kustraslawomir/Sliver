package app.sliver.data.backup

import app.sliver.data.settings.AccountSettings

enum class NetworkKind { NONE, METERED, UNMETERED }

enum class BackupUi { BACKING_UP, UP_TO_DATE, PAUSED_WIFI, FAILED, DRIVE_FULL, REVOKED, OFFLINE }

data class BackupSnapshot(
    val pending: Int,
    val running: Boolean,
    val error: BackupError?,
    val wifiOnly: Boolean,
    val network: NetworkKind,
)

fun backupUi(s: BackupSnapshot): BackupUi = when {
    s.error == BackupError.REVOKED -> BackupUi.REVOKED
    s.running -> BackupUi.BACKING_UP
    s.pending == 0 -> BackupUi.UP_TO_DATE
    s.error == BackupError.DRIVE_FULL -> BackupUi.DRIVE_FULL
    s.error == BackupError.OFFLINE || s.network == NetworkKind.NONE -> BackupUi.OFFLINE
    s.wifiOnly && s.network == NetworkKind.METERED -> BackupUi.PAUSED_WIFI
    s.error != null -> BackupUi.FAILED
    else -> BackupUi.BACKING_UP
}

const val PROBLEM_AFTER_MILLIS = 24L * 60 * 60 * 1000

/** A backup problem is surfaced only once failures have lasted 24 h, and only once per failing streak. */
fun shouldNotifyBackupProblem(account: AccountSettings, enabled: Boolean, nowMillis: Long): Boolean {
    val since = account.failingSince ?: return false
    return enabled && account.backupError != null && !account.problemNotified && nowMillis - since >= PROBLEM_AFTER_MILLIS
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1_000_000L -> "${bytes / 1000} KB"
    bytes < 1_000_000_000L -> "${tenths(bytes, 1_000_000L)} MB"
    else -> "${tenths(bytes, 1_000_000_000L)} GB"
}

private fun tenths(v: Long, unit: Long): String {
    val t = v * 10 / unit
    return "${t / 10}.${t % 10}"
}
