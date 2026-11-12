package app.sliver

import app.sliver.data.backup.BackupError
import app.sliver.data.backup.BackupSnapshot
import app.sliver.data.backup.BackupUi
import app.sliver.data.backup.NetworkKind
import app.sliver.data.backup.PROBLEM_AFTER_MILLIS
import app.sliver.data.backup.backupUi
import app.sliver.data.backup.buildManifest
import app.sliver.data.backup.formatBytes
import app.sliver.data.backup.shouldNotifyBackupProblem
import app.sliver.data.settings.AccountSettings
import app.sliver.data.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun snap(
    pending: Int = 1, running: Boolean = false, error: BackupError? = null,
    wifiOnly: Boolean = true, network: NetworkKind = NetworkKind.UNMETERED,
) = BackupSnapshot(pending, running, error, wifiOnly, network)

class BackupStateTest {
    @Test
    fun mapsAllSevenStates() {
        assertEquals(BackupUi.BACKING_UP, backupUi(snap(running = true)))
        assertEquals(BackupUi.UP_TO_DATE, backupUi(snap(pending = 0)))
        assertEquals(BackupUi.PAUSED_WIFI, backupUi(snap(network = NetworkKind.METERED)))
        assertEquals(BackupUi.FAILED, backupUi(snap(error = BackupError.OTHER)))
        assertEquals(BackupUi.DRIVE_FULL, backupUi(snap(error = BackupError.DRIVE_FULL)))
        assertEquals(BackupUi.REVOKED, backupUi(snap(error = BackupError.REVOKED)))
        assertEquals(BackupUi.OFFLINE, backupUi(snap(network = NetworkKind.NONE)))
    }

    @Test
    fun meteredIsFineWhenWifiOnlyIsOff() {
        assertEquals(BackupUi.BACKING_UP, backupUi(snap(wifiOnly = false, network = NetworkKind.METERED)))
    }

    @Test
    fun problemNotificationOnlyAfter24HoursOnce() {
        val a = AccountSettings(backupError = BackupError.DRIVE_FULL, failingSince = 1000L)
        assertFalse(shouldNotifyBackupProblem(a, true, 1000L + PROBLEM_AFTER_MILLIS - 1))
        assertTrue(shouldNotifyBackupProblem(a, true, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(a, false, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(a.copy(problemNotified = true), true, 1000L + PROBLEM_AFTER_MILLIS))
        assertFalse(shouldNotifyBackupProblem(AccountSettings(), true, Long.MAX_VALUE))
    }

    @Test
    fun formatsBytes() {
        assertEquals("512 KB", formatBytes(512_000))
        assertEquals("12.3 MB", formatBytes(12_340_000))
        assertEquals("15.0 GB", formatBytes(15_000_000_000))
    }
}

class ManifestTest {
    @Test
    fun manifestHasSettingsAndFilms() {
        val json = buildManifest(AppSettings(), emptyList(), 5L)
        assertTrue(json.contains("\"dateStamp\":true"))
        assertTrue(json.contains("\"films\":[]"))
    }
}
