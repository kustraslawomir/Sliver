package app.sliver.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.sliver.core.navigation.Route
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Notices
import app.sliver.data.files.FileStore
import app.sliver.feature.camera.CaptureSession
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

/**
 * Opens the system Photo Picker straight away for [forDate] (null = today). Media recorded on another day is refused with a notice. A video or Motion Photo goes to Edit clip,
 * a photo to Photo clip. Cancelling leaves the user where they were.
 */
@Composable
fun rememberAddMedia(onNavigate: (Route) -> Unit): (LocalDate?) -> Unit {
    val provider = remember { KoinPlatform.getKoin().get<GalleryProvider>() }
    val files = remember { KoinPlatform.getKoin().get<FileStore>() }
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<LocalDate?>(null) }
    var busy by remember { mutableStateOf(false) }

    val launchPicker = rememberMediaPicker { uri ->
        if (busy) return@rememberMediaPicker
        busy = true
        val forDate = target
        scope.launch {
            val captured = runCatching { provider.describe(listOf(uri)).firstOrNull()?.date }.getOrNull()
            val check = checkCaptureDate(captured, forDate ?: todayLocal())
            if (check is AddCheck.Blocked) {
                busy = false
                Notices.show(check.message)
                return@launch
            }
            val route = loadIntoSession(provider, files, uri, forDate)
            busy = false
            if (route == null) {
                Notices.show("Couldn't read that item. Pick another.")
                return@launch
            }
            onNavigate(route)
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
