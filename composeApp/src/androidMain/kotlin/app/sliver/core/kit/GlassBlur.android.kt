package app.sliver.core.kit

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberGlassBlur(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val power = LocalContext.current.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return power?.isPowerSaveMode != true
}
