package app.sliver.feature.settings

import androidx.compose.runtime.Composable

interface SettingsPlatform {
    val versionName: String
    fun sendFeedback(address: String, subject: String)

    /** Whether Android lets this app post notifications (permission granted and not switched off in system settings). */
    fun notificationsEnabled(): Boolean
    fun openNotificationSettings()
    val is24Hour: Boolean
}

@Composable
expect fun rememberSettingsPlatform(): SettingsPlatform
