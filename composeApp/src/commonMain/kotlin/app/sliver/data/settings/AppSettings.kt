package app.sliver.data.settings

import app.sliver.data.FilmFormat
import app.sliver.data.backup.BackupError
import app.sliver.data.LookPreset
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

enum class ReminderMode { FIXED, RANDOM, OFF }
enum class CameraMode { VIDEO, PHOTO }
enum class Lens { BACK, FRONT }

data class ReminderSettings(
    val mode: ReminderMode = ReminderMode.FIXED,
    val time: LocalTime = LocalTime(19, 30),
    val windowStart: LocalTime = LocalTime(10, 0),
    val windowEnd: LocalTime = LocalTime(20, 0),
    val days: List<Boolean> = List(7) { true },
)

data class NotifSettings(
    val runningLate: Boolean = false,
    val filmReady: Boolean = true,
    val backupProblems: Boolean = true,
    val onThisDay: Boolean = true,
)

data class CameraSettings(
    val mode: CameraMode = CameraMode.VIDEO,
    val look: LookPreset = LookPreset.NATURAL,
    val lens: Lens = Lens.BACK,
    val flash: Boolean = false,
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
)

data class AppSettings(
    val defaultLook: LookPreset = LookPreset.NATURAL,
    val newFilmFormat: FilmFormat = FilmFormat.CINEMA,
    val dateStamp: Boolean = true,
    val saveFullRecordings: Boolean = false,
    val volumeKeyRecords: Boolean = false,
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
