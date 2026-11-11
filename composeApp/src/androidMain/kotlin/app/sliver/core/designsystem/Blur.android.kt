package app.sliver.core.designsystem

import android.os.Build

actual fun blurSupported(): Boolean = Build.VERSION.SDK_INT >= 31
