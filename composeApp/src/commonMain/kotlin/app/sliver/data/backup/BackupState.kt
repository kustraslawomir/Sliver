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

/** "Synced · 2 min ago · 2 phones" */
fun syncStatusLine(lastSyncAt: Long?, nowMillis: Long, phones: Int): String {
    if (lastSyncAt == null) return "Not synced yet"
    val minutes = ((nowMillis - lastSyncAt).coerceAtLeast(0) / 60_000).toInt()
    val ago = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
    return "Synced · $ago · $phones ${if (phones == 1) "phone" else "phones"}"
}

private val ShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** One-time notice after another phone replaced clips here. */
fun changedOnAnotherPhone(dates: List<kotlinx.datetime.LocalDate>): String? = when (dates.size) {
    0 -> null
    1 -> "${dates[0].day} ${ShortMonths[dates[0].month.ordinal]} was changed on another phone"
    else -> "${dates.size} clips were changed on another phone"
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

private fun clips(n: Int) = if (n == 1) "1 clip" else "$n clips"

/** Clips row on Backup & sync: "No clips yet", "12 clips in Drive", "3 of 12 clips in Drive". */
fun clipsLine(backedUp: Int, total: Int): String = when {
    total == 0 -> "No clips yet"
    backedUp >= total -> "${clips(total)} in Drive"
    else -> "$backedUp of ${clips(total)} in Drive"
}

/** Drive row: free space, flagged when under 1 GB so the screen can warn with an icon, not colour alone. */
data class DriveLine(val text: String, val low: Boolean)

const val DRIVE_LOW_BYTES = 1L shl 30

fun driveLine(used: Long, limit: Long?): DriveLine {
    if (limit == null) return DriveLine("${formatBytes(used)} used", false)
    val free = (limit - used).coerceAtLeast(0)
    val low = free < DRIVE_LOW_BYTES
    return DriveLine(if (low) "Almost full · ${formatBytes(free)} free" else "${formatBytes(free)} free", low)
}
