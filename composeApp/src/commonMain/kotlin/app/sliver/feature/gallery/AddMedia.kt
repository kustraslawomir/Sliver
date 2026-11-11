package app.sliver.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.sliver.core.navigation.Route
import app.sliver.core.ui.Notices
import app.sliver.data.DaySource
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

/**
 * Opens the system Photo Picker straight away for [forDate] (null = today). A video or Motion Photo goes to Edit clip,
 * a photo to Photo clip. Cancelling leaves the user where they were.
 */
@Composable
fun rememberAddMedia(onNavigate: (Route) -> Unit): (LocalDate?) -> Unit {
    val provider = remember { KoinPlatform.getKoin().get<GalleryProvider>() }
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<LocalDate?>(null) }
    var busy by remember { mutableStateOf(false) }

    val launchPicker = rememberMediaPicker { uri ->
        if (busy) return@rememberMediaPicker
        busy = true
        val forDate = target
        scope.launch {
            val motion = runCatching { provider.extractMotionVideo(uri) }.getOrNull()
            val video = motion != null || provider.isVideo(uri)
            val path = motion?.path ?: runCatching { provider.copyToTmp(uri) }.getOrNull()
            busy = false
            if (path == null) {
                Notices.show("Couldn't read that item. Pick another.")
                return@launch
            }
            CaptureSession.path = path
            CaptureSession.forDate = forDate
            CaptureSession.motion = motion != null
            CaptureSession.motionPresentationUs = motion?.presentationUs
            CaptureSession.source = if (video) DaySource.GALLERY else DaySource.PHOTO
            onNavigate(if (video) Route.Trim else Route.PhotoTrim)
        }
    }
    return { date ->
        if (!busy) {
            target = date
            CaptureSession.forDate = date
            launchPicker()
        }
    }
}
