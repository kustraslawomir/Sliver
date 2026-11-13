package app.sliver.feature.settings

import app.sliver.core.theme.ReelogType

import app.sliver.core.APP_NAME

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.feature.camera.label
import app.sliver.feature.notifications.reminderSummary
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.SwitchRow
import app.sliver.data.FilmFormat
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.Appearance
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

internal const val FEEDBACK_ADDRESS = "feedback@sliver.app"
private val Formats = listOf(FilmFormat.CINEMA, FilmFormat.STORY)
private val Appearances = listOf(Appearance.SYSTEM to "System", Appearance.LIGHT to "Light", Appearance.DARK to "Dark")

@Composable
fun SettingsScreen(onGo: (Route) -> Unit, onBack: () -> Unit) {
    val repo = remember { KoinPlatform.getKoin().get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val platform = rememberSettingsPlatform()
    val location = rememberPermissionController()
    val settings by remember { repo.settings }.collectAsState(null)
    var showStores by remember { mutableStateOf(false) }
    val s = settings ?: return Column(Modifier.fillMaxSize().background(SliverColors.BgBase)) {}

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { repo.update(transform) }
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S6)
            .padding(bottom = SliverSpace.S10 + SliverSpace.S8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            SliverText("Settings", SliverType.DisplayM)
        }

        Group("Account & backup")
        NavRow("Account", if (s.account.signedIn) "Google" else "Not signed in") { onGo(Route.Backup) }
        NavRow("Backup & sync", if (s.account.signedIn && s.account.autoBackup) "On" else "Off") { onGo(Route.Backup) }

        Group("Films & clips")
        ChoiceRow(
            "Default film format", Formats.map { it.label() }, Formats.indexOf(s.newFilmFormat),
        ) { i -> update { it.copy(newFilmFormat = Formats[i]) } }
        LinedSwitch(
            "Date stamp", s.dateStamp, { on -> update { it.copy(dateStamp = on) } },
            "On every clip. You can still turn it off for a film.",
        )
        LinedSwitch(
            "Also save full recordings to Photos", s.saveFullRecordings, { on -> update { it.copy(saveFullRecordings = on) } },
            "Off: only the chosen clip is kept, the rest is deleted.",
        )
        // DECISION: the volume-key option lives in Settings (not on the Camera toolbar) and only acts while Camera is open.
        LinedSwitch(
            "Volume key records", s.volumeKeyRecords, { on -> update { it.copy(volumeKeyRecords = on) } },
            "Press a volume key on the camera to start and stop.",
        )
        LinedSwitch(
            "Show golden hour", s.showGoldenHour,
            { on ->
                update { it.copy(showGoldenHour = on) }
                if (on && location.status(PermissionKind.CoarseLocation) == PermissionStatus.NotAsked) location.request(PermissionKind.CoarseLocation)
            },
            "A line on the camera in the 3 hours before sunset. Uses rough location.",
        )

        Group("App")
        ChoiceRow(
            "Appearance", Appearances.map { it.second }, Appearances.indexOfFirst { it.first == s.appearance },
        ) { i -> update { it.copy(appearance = Appearances[i].first) } }
        NavRow("Daily reminder", reminderSummary(s.reminder)) { onGo(Route.Reminders) }
        NavRow("Fill the calendar", null, "Put photos and videos from your gallery on empty days") { onGo(Route.FillThePast) }

        Group("About")
        NavRow("What $APP_NAME stores", null) { showStores = !showStores }
        if (showStores) {
            SliverText(
                "Your clips, photos and films stay on this phone. If you turn on backup, copies go to a private folder in your own Google Drive. Nothing is shared with us.",
                SliverType.BodySmall, Modifier.padding(bottom = SliverSpace.S3), color = SliverColors.TextSecondary,
            )
        }
        NavRow("Send feedback", null) { platform.sendFeedback(FEEDBACK_ADDRESS, "$APP_NAME feedback") }

        SliverText(
            "$APP_NAME ${platform.versionName}", SliverType.Meta,
            Modifier.padding(top = SliverSpace.S6).combinedClickable(onClick = {}, onLongClick = { platform.seedDebugData() }),
            color = SliverColors.TextTertiary,
        )
    }
}

@Composable
private fun Group(title: String) {
    SliverText(
        title, SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)),
        Modifier.padding(top = SliverSpace.S6, bottom = SliverSpace.S3), color = SliverColors.TextTertiary,
    )
}

@Composable
private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))

@Composable
private fun LinedSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit, caption: String) {
    Hairline()
    SwitchRow(title, checked, onChange, caption = caption)
}

private val ValueStyle @Composable get() = SliverType.Caption.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption))

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Hairline()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { open = !open }.padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        SliverText(title, SliverType.Body, Modifier.weight(1f))
        SliverText(options.getOrElse(selected) { "" }, ValueStyle, color = SliverColors.TextTertiary)
        SliverIcon(if (open) SliverIcons.ChevronDown else SliverIcons.ChevronRight, SliverColors.TextTertiary)
    }
    if (open) options.forEachIndexed { i, label ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(i); open = false }.padding(start = SliverSpace.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SliverText(label, SliverType.Body, Modifier.weight(1f), if (i == selected) SliverColors.AccentText else SliverColors.TextSecondary)
            if (i == selected) SliverIcon(SliverIcons.Check, SliverColors.AccentText)
        }
    }
}

@Composable
private fun NavRow(title: String, value: String?, caption: String? = null, onClick: () -> Unit) {
    Hairline()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Column(Modifier.weight(1f)) {
            SliverText(title, SliverType.Body)
            if (caption != null) SliverText(caption, SliverType.Caption)
        }
        if (value != null) SliverText(value, ValueStyle, color = SliverColors.TextTertiary)
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary)
    }
}
