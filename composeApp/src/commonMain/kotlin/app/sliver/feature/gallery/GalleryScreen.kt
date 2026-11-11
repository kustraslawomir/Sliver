package app.sliver.feature.gallery

import app.sliver.core.ui.SlitLoader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.LocalNavigator
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Chip
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.data.DaySource
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.trim.dayLabel
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/** Context for the system Photo Picker: says which day the pick is for, then launches the picker. */
@Composable
fun GalleryScreen(onNavigate: (Route) -> Unit) {
    val navigator = LocalNavigator.current
    val provider = remember { KoinPlatform.getKoin().get<GalleryProvider>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }
    val forDate = remember { CaptureSession.forDate }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val launchPicker = rememberMediaPicker { uri ->
        if (busy) return@rememberMediaPicker
        busy = true
        failed = false
        scope.launch {
            val motion = runCatching { provider.extractMotionVideo(uri) }.getOrNull()
            val video = motion != null || provider.isVideo(uri)
            val path = motion?.path ?: runCatching { provider.copyToTmp(uri) }.getOrNull()
            busy = false
            if (path == null) {
                failed = true
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

    Column(Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton44({ navigator.back() }, "Close") {
                SliverIcon(SliverIcons.X, SliverColors.TextPrimary)
            }
            SliverText("Add media", SliverType.Headline, Modifier.weight(1f), SliverColors.TextPrimary, maxLines = 1)
            if (forDate != null) Chip("FOR " + dayLabel(forDate, today).uppercase(), selected = true, onClick = {})
        }
        Column(
            Modifier.fillMaxSize().padding(SliverSpace.Gutter),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S4, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SliverText(
                if (forDate != null) "Pick the photo or video for this day." else "Pick a photo or video.",
                SliverType.Body, color = SliverColors.TextSecondary,
            )
            if (failed) SliverText("Could not read that file. Try another.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            if (busy) SlitLoader()
            PrimaryButton(if (busy) "Preparing..." else "Choose from gallery", { if (!busy) launchPicker() }, Modifier.fillMaxWidth())
        }
    }
}
