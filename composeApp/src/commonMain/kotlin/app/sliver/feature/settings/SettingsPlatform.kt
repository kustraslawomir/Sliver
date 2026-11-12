package app.sliver.feature.settings

import androidx.compose.runtime.Composable

interface SettingsPlatform {
    val versionName: String
    fun sendFeedback(address: String, subject: String)

    /** Whether Android lets this app post notifications (permission granted and not switched off in system settings). */
    fun notificationsEnabled(): Boolean
    fun openNotificationSettings()
    val is24Hour: Boolean

    /** Debug builds only: fills the app with synthetic clips (long-press on the version row). No-op in release. */
    fun seedDebugData() {}
}

@Composable
expect fun rememberSettingsPlatform(): SettingsPlatform
