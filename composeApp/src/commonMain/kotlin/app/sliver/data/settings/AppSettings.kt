package app.sliver.data.settings

import app.sliver.data.FilmFormat
import app.sliver.data.LookPreset
import app.sliver.data.backup.BackupError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

enum class CameraMode { VIDEO, PHOTO }
enum class Appearance { SYSTEM, LIGHT, DARK }
enum class Lens { BACK, FRONT }

val LastCallTime = LocalTime(22, 30)

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime(20, 0),
    val days: List<Boolean> = List(7) { true },
    val skipIfDone: Boolean = true,
    val lastCall: Boolean = false,
    val followGoldenHour: Boolean = false,
    /** Ids of the projects that count; empty means every open project. */
    val projects: Set<String> = emptySet(),
)

data class NotifSettings(
    val filmReady: Boolean = true,
    val backupProblems: Boolean = true,
    val onThisDay: Boolean = true,
)

data class CameraSettings(
    val look: LookPreset = LookPreset.NATURAL,
    val grid: Boolean = false,
    val activeFilmId: String? = null,
)

data class AccountSettings(
    val signedIn: Boolean = false,
    val email: String? = null,
    val driveFolderId: String? = null,
    val autoBackup: Boolean = true,
    val wifiOnly: Boolean = true,
    val backupError: BackupError? = null,
    val failingSince: Long? = null,
    val problemNotified: Boolean = false,
    val pendingTrash: List<String> = emptyList(),
    /** Stable id of this install in the shared Drive; never synced. */
    val deviceId: String = "",
    val lastSyncAt: Long? = null,
    /** Start time of the last cycle that finished; local edits after it still have to be pushed. */
    val lastPushedAt: Long = 0,
    /** Distinct phones seen on Drive in the last 30 days, this one included. */
    val phones: Int = 1,
    /** "projectId|date" of days another phone replaced since the notice was last shown. */
    val changedNotices: List<String> = emptyList(),
)

data class AppSettings(
    val defaultLook: LookPreset = LookPreset.NATURAL,
    val newFilmFormat: FilmFormat = FilmFormat.CINEMA,
    val appearance: Appearance = Appearance.SYSTEM,
    val dateStamp: Boolean = true,
    /** Owner 13 Nov: on by default; recordings also land in Movies/<app>. */
    val saveFullRecordings: Boolean = true,
    val volumeKeyRecords: Boolean = false,
    val showGoldenHour: Boolean = false,
    val reminder: ReminderSettings = ReminderSettings(),
    val notif: NotifSettings = NotifSettings(),
    val camera: CameraSettings = CameraSettings(),
    val onboardingDone: Boolean = false,
    val coachTrimSeen: Boolean = false,
    val coachRecordSeen: Boolean = false,
    val nudgeDismissed: Boolean = false,
    val firstDay: LocalDate? = null,
    val activeProjectId: String? = null,
    val account: AccountSettings = AccountSettings(),
)
