package app.sliver.core.log

import android.util.Log
import app.sliver.BuildConfig

actual fun logDebug(tag: String, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag, message)
}
