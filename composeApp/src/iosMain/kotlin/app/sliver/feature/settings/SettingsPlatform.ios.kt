package app.sliver.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

@Composable
actual fun rememberSettingsPlatform(): SettingsPlatform = remember {
    object : SettingsPlatform {
        override val versionName: String =
            NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""

        override fun sendFeedback(address: String, subject: String) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            val s = (subject as NSString).stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet) ?: ""
            NSURL.URLWithString("mailto:$address?subject=$s")?.let {
                UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
            }
        }
    }
}
