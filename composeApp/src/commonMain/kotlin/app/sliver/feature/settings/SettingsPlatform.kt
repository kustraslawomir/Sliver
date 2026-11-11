package app.sliver.feature.settings

import androidx.compose.runtime.Composable

interface SettingsPlatform {
    val versionName: String
    fun sendFeedback(address: String, subject: String)
}

@Composable
expect fun rememberSettingsPlatform(): SettingsPlatform
