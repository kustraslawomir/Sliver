package app.sliver.core.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPermissionController(): PermissionController {
    var version by remember { mutableIntStateOf(0) }
    var notif by remember { mutableIntStateOf(-1) }
    val bump = { dispatch_async(dispatch_get_main_queue()) { version++ } }
    return remember(version, notif) {
        object : PermissionController {
            override fun status(kind: PermissionKind): PermissionStatus = when (kind) {
                PermissionKind.CoarseLocation -> TODO("iOS later")
                PermissionKind.CameraMic -> {
                    val v = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                    val a = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)
                    when {
                        v == AVAuthorizationStatusAuthorized && a == AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
                        v == AVAuthorizationStatusNotDetermined || a == AVAuthorizationStatusNotDetermined -> PermissionStatus.NotAsked
                        else -> PermissionStatus.Denied
                    }
                }
                PermissionKind.Notifications -> {
                    if (notif < 0) UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { s ->
                        val st = s?.authorizationStatus?.toInt() ?: 0
                        dispatch_async(dispatch_get_main_queue()) { notif = st }
                    }
                    when (notif.toLong()) {
                        UNAuthorizationStatusAuthorized -> PermissionStatus.Granted
                        UNAuthorizationStatusDenied -> PermissionStatus.Denied
                        else -> PermissionStatus.NotAsked
                    }
                }
            }

            override fun request(kind: PermissionKind) {
                when (kind) {
                    PermissionKind.CoarseLocation -> TODO("iOS later")
                    PermissionKind.CameraMic -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { bump() }
                    }
                    PermissionKind.Notifications -> UNUserNotificationCenter.currentNotificationCenter()
                        .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ ->
                            dispatch_async(dispatch_get_main_queue()) { notif = -1 }
                        }
                }
            }

            override fun openSettings() {
                NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                    UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
                }
            }
        }
    }
}
