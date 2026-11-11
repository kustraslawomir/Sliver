package app.sliver.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class AndroidSettingsPlatform(private val context: Context) : SettingsPlatform {
    override val versionName: String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    override fun sendFeedback(address: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address?subject=${Uri.encode(subject)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

@Composable
actual fun rememberSettingsPlatform(): SettingsPlatform {
    val context = LocalContext.current
    return remember(context) { AndroidSettingsPlatform(context) }
}
