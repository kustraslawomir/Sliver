package app.sliver.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.sliver.data.backup.BackupError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private object K {
        val defaultLook = stringPreferencesKey("defaultLook")
        val newFilmFormat = stringPreferencesKey("newFilmFormat")
        val appearance = stringPreferencesKey("appearance")
        val dateStamp = booleanPreferencesKey("dateStamp")
        val saveFullRecordings = booleanPreferencesKey("saveFullRecordings")
        val volumeKeyRecords = booleanPreferencesKey("volumeKeyRecords")
        val showGoldenHour = booleanPreferencesKey("showGoldenHour")
        val reminderEnabled = booleanPreferencesKey("reminder.enabled")
        val reminderTime = stringPreferencesKey("reminder.time")
        val reminderDays = stringPreferencesKey("reminder.days")
        val reminderSkipIfDone = booleanPreferencesKey("reminder.skipIfDone")
        val reminderLastCall = booleanPreferencesKey("reminder.lastCall")
        val reminderFollowGolden = booleanPreferencesKey("reminder.followGoldenHour")
        val reminderProjects = stringPreferencesKey("reminder.projects")
        val notifFilmReady = booleanPreferencesKey("notif.filmReady")
        val notifBackupProblems = booleanPreferencesKey("notif.backupProblems")
        val notifOnThisDay = booleanPreferencesKey("notif.onThisDay")
        val cameraLook = stringPreferencesKey("camera.look")
        val cameraGrid = booleanPreferencesKey("camera.grid")
        val cameraActiveFilmId = stringPreferencesKey("camera.activeFilmId")
        val onboardingDone = booleanPreferencesKey("onboardingDone")
        val coachTrimSeen = booleanPreferencesKey("coachTrimSeen")
        val coachRecordSeen = booleanPreferencesKey("coachRecordSeen")
        val nudgeDismissed = booleanPreferencesKey("nudgeDismissed")
        val firstDay = stringPreferencesKey("firstDay")
        val activeProjectId = stringPreferencesKey("activeProjectId")
        val accountSignedIn = booleanPreferencesKey("account.signedIn")
        val accountEmail = stringPreferencesKey("account.email")
        val accountDriveFolderId = stringPreferencesKey("account.driveFolderId")
        val accountAutoBackup = booleanPreferencesKey("account.autoBackup")
        val accountWifiOnly = booleanPreferencesKey("account.wifiOnly")
        val accountBackupError = stringPreferencesKey("account.backupError")
        val accountFailingSince = longPreferencesKey("account.failingSince")
        val accountProblemNotified = booleanPreferencesKey("account.problemNotified")
        val accountPendingTrash = stringPreferencesKey("account.pendingTrash")
        val accountDeviceId = stringPreferencesKey("account.deviceId")
        val accountLastSyncAt = longPreferencesKey("account.lastSyncAt")
        val accountLastPushedAt = longPreferencesKey("account.lastPushedAt")
        val accountPhones = intPreferencesKey("account.phones")
        val accountChangedNotices = stringPreferencesKey("account.changedNotices")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            defaultLook = enum(K.defaultLook, d.defaultLook),
            newFilmFormat = enum(K.newFilmFormat, d.newFilmFormat),
            appearance = enum(K.appearance, d.appearance),
            dateStamp = this[K.dateStamp] ?: d.dateStamp,
            saveFullRecordings = this[K.saveFullRecordings] ?: d.saveFullRecordings,
            volumeKeyRecords = this[K.volumeKeyRecords] ?: d.volumeKeyRecords,
            showGoldenHour = this[K.showGoldenHour] ?: d.showGoldenHour,
            reminder = ReminderSettings(
                enabled = this[K.reminderEnabled] ?: d.reminder.enabled,
                time = this[K.reminderTime]?.let(LocalTime::parse) ?: d.reminder.time,
                days = this[K.reminderDays]?.takeIf { it.length == 7 }?.map { it == '1' } ?: d.reminder.days,
                skipIfDone = this[K.reminderSkipIfDone] ?: d.reminder.skipIfDone,
                lastCall = this[K.reminderLastCall] ?: d.reminder.lastCall,
                followGoldenHour = this[K.reminderFollowGolden] ?: d.reminder.followGoldenHour,
                projects = this[K.reminderProjects]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: d.reminder.projects,
            ),
            notif = NotifSettings(
                filmReady = this[K.notifFilmReady] ?: d.notif.filmReady,
                backupProblems = this[K.notifBackupProblems] ?: d.notif.backupProblems,
                onThisDay = this[K.notifOnThisDay] ?: d.notif.onThisDay,
            ),
            camera = CameraSettings(
                look = enum(K.cameraLook, d.camera.look),
                grid = this[K.cameraGrid] ?: d.camera.grid,
                activeFilmId = this[K.cameraActiveFilmId],
            ),
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            coachTrimSeen = this[K.coachTrimSeen] ?: d.coachTrimSeen,
            coachRecordSeen = this[K.coachRecordSeen] ?: d.coachRecordSeen,
            nudgeDismissed = this[K.nudgeDismissed] ?: d.nudgeDismissed,
            firstDay = this[K.firstDay]?.let(LocalDate::parse),
            activeProjectId = this[K.activeProjectId],
            account = AccountSettings(
                signedIn = this[K.accountSignedIn] ?: d.account.signedIn,
                email = this[K.accountEmail],
                driveFolderId = this[K.accountDriveFolderId],
                autoBackup = this[K.accountAutoBackup] ?: d.account.autoBackup,
                wifiOnly = this[K.accountWifiOnly] ?: d.account.wifiOnly,
                backupError = this[K.accountBackupError]?.let { n -> BackupError.entries.firstOrNull { it.name == n } },
                failingSince = this[K.accountFailingSince],
                problemNotified = this[K.accountProblemNotified] ?: false,
                pendingTrash = this[K.accountPendingTrash]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                deviceId = this[K.accountDeviceId].orEmpty(),
                lastSyncAt = this[K.accountLastSyncAt],
                lastPushedAt = this[K.accountLastPushedAt] ?: 0,
                phones = this[K.accountPhones] ?: 1,
                changedNotices = this[K.accountChangedNotices]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            ),
        )
    }

    private fun <T> MutablePreferences.put(key: Preferences.Key<T>, v: T?) {
        if (v == null) remove(key) else this[key] = v
    }

    private fun MutablePreferences.write(s: AppSettings) {
        put(K.defaultLook, s.defaultLook.name)
        put(K.newFilmFormat, s.newFilmFormat.name)
        put(K.appearance, s.appearance.name)
        put(K.dateStamp, s.dateStamp)
        put(K.saveFullRecordings, s.saveFullRecordings)
        put(K.volumeKeyRecords, s.volumeKeyRecords)
        put(K.showGoldenHour, s.showGoldenHour)
        put(K.reminderEnabled, s.reminder.enabled)
        put(K.reminderTime, s.reminder.time.toString())
        put(K.reminderDays, s.reminder.days.joinToString("") { if (it) "1" else "0" })
        put(K.reminderSkipIfDone, s.reminder.skipIfDone)
        put(K.reminderLastCall, s.reminder.lastCall)
        put(K.reminderFollowGolden, s.reminder.followGoldenHour)
        put(K.reminderProjects, s.reminder.projects.joinToString(","))
        put(K.notifFilmReady, s.notif.filmReady)
        put(K.notifBackupProblems, s.notif.backupProblems)
        put(K.notifOnThisDay, s.notif.onThisDay)
        put(K.cameraLook, s.camera.look.name)
        put(K.cameraGrid, s.camera.grid)
        put(K.cameraActiveFilmId, s.camera.activeFilmId)
        put(K.onboardingDone, s.onboardingDone)
        put(K.coachTrimSeen, s.coachTrimSeen)
        put(K.coachRecordSeen, s.coachRecordSeen)
        put(K.nudgeDismissed, s.nudgeDismissed)
        put(K.firstDay, s.firstDay?.toString())
        put(K.activeProjectId, s.activeProjectId)
        put(K.accountSignedIn, s.account.signedIn)
        put(K.accountEmail, s.account.email)
        put(K.accountDriveFolderId, s.account.driveFolderId)
        put(K.accountAutoBackup, s.account.autoBackup)
        put(K.accountWifiOnly, s.account.wifiOnly)
        put(K.accountBackupError, s.account.backupError?.name)
        put(K.accountFailingSince, s.account.failingSince)
        put(K.accountProblemNotified, s.account.problemNotified)
        put(K.accountPendingTrash, s.account.pendingTrash.joinToString(","))
        put(K.accountDeviceId, s.account.deviceId)
        put(K.accountLastSyncAt, s.account.lastSyncAt)
        put(K.accountLastPushedAt, s.account.lastPushedAt)
        put(K.accountPhones, s.account.phones)
        put(K.accountChangedNotices, s.account.changedNotices.joinToString(","))
    }
}
