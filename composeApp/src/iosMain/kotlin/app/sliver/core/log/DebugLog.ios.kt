package app.sliver.core.log

import platform.Foundation.NSLog

actual fun logDebug(tag: String, message: String) {
    NSLog("%@: %@", tag, message)
}
