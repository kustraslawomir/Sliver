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
 * Opens the system Photo Picker (multi-select, so a Live Photo's still and .mov can be picked together) for [forDate] (null = today).
 * Media with a known capture date on another day is refused; a date from the file name or no date is added to [forDate] with a notice.
 * A video, Motion Photo or Live Photo goes to Edit clip, a photo to Photo clip. Cancelling leaves the user where they were.
 */
@Composable
fun rememberAddMedia(onNavigate: (Route) -> Unit, preferDayGallery: Boolean = true): (LocalDate?) -> Unit {
    // Owner: "can we filter the gallery to the chosen date?" The system picker can't filter by date, so when we already
    // have gallery access (granted for auto-import) a past day opens our own "from this day" grid instead.
    val galleryStatus = app.sliver.core.permissions.rememberPermissionController().status(app.sliver.core.permissions.PermissionKind.MediaLibrary)
    val hasGallery = galleryStatus == app.sliver.core.permissions.PermissionStatus.Granted || galleryStatus == app.sliver.core.permissions.PermissionStatus.Partial
    val provider = remember { KoinPlatform.getKoin().get<GalleryProvider>() }
    val files = remember { KoinPlatform.getKoin().get<FileStore>() }
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<LocalDate?>(null) }
    var busy by remember { mutableStateOf(false) }

    val launchPicker = rememberMultiMediaPicker { uris ->
        if (busy) return@rememberMultiMediaPicker
        busy = true
        val forDate = target
        scope.launch {
            val group = groupLivePhotos(runCatching { provider.describe(uris) }.getOrDefault(emptyList())).singleOrNull()
            if (group == null) {
                busy = false
                Notices.show("Pick one clip at a time, or a photo with its Live Photo video.")
                return@launch
            }
            val check = checkCaptureDate(group.primary.captured, forDate ?: todayLocal())
            if (check is AddCheck.Blocked) {
                busy = false
                Notices.show(check.message)
                return@launch
            }
            val route = loadIntoSession(provider, files, group.primary.uri, forDate, liveVideo = group.liveVideo?.uri)
            busy = false
            if (route == null) {
                Notices.show("Couldn't read that item. Pick another.")
                return@launch
            }
            if (check is AddCheck.AllowedWithNotice) Notices.show(check.message)
            onNavigate(route)
        }
    }
    return { date ->
        if (!busy) {
            target = date
            CaptureSession.forDate = date
            if (preferDayGallery && hasGallery && date != null && date < todayLocal()) onNavigate(Route.DayGallery) else launchPicker()
        }
    }
}
