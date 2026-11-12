package app.sliver.feature.settings

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.Chip
import app.sliver.feature.camera.label
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

        Group("Backup")
        NavRow("Account", if (s.account.signedIn) s.account.email ?: "Signed in" else "Not signed in") { onGo(Route.Backup) }
        NavRow("Backup", if (s.account.signedIn && s.account.autoBackup) "On" else "Off") { onGo(Route.Backup) }

        Group("Films")
        ChoiceRow(
            "Default film format", Formats.map { it.label() }, Formats.indexOf(s.newFilmFormat),
        ) { i -> update { it.copy(newFilmFormat = Formats[i]) } }

        Group("Appearance")
        ChoiceRow(
            "Theme", Appearances.map { it.second }, Appearances.indexOfFirst { it.first == s.appearance },
        ) { i -> update { it.copy(appearance = Appearances[i].first) } }

        Group("Capture")
        SwitchRow("Date stamp", s.dateStamp, { on -> update { it.copy(dateStamp = on) } })
        SwitchRow(
            "Also save full recordings to Photos", s.saveFullRecordings,
            { on -> update { it.copy(saveFullRecordings = on) } },
        )

        // DECISION: the volume-key option lives in Settings (not on the Camera toolbar) and only acts while Camera is open.
        SwitchRow("Volume key records", s.volumeKeyRecords, { on -> update { it.copy(volumeKeyRecords = on) } })

        Group("More")
        NavRow("Notifications", null) { onGo(Route.Notifications) }
        NavRow("Fill empty days", null) { onGo(Route.FillThePast) }
        NavRow("What SLIVER stores", null) { showStores = !showStores }
        if (showStores) {
            SliverText(
                "Your clips, photos and films stay on this phone. If you turn on backup, copies go to a private folder in your own Google Drive. Nothing is shared with us.",
                SliverType.BodySmall, Modifier.padding(bottom = SliverSpace.S3), color = SliverColors.TextSecondary,
            )
        }
        NavRow("Send feedback", null) { platform.sendFeedback(FEEDBACK_ADDRESS, "SLIVER feedback") }

        SliverText(
            "Sliver ${platform.versionName}", SliverType.Meta, Modifier.padding(top = SliverSpace.S6),
            color = SliverColors.TextTertiary,
        )
    }
}

@Composable
private fun Group(title: String) {
    SliverText(title, SliverType.Caption, Modifier.padding(top = SliverSpace.S6, bottom = SliverSpace.S1), color = SliverColors.TextTertiary)
}

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { open = !open }.padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        SliverText(title, SliverType.Body, Modifier.weight(1f))
        SliverText(options.getOrElse(selected) { "" }, SliverType.BodySmall, color = SliverColors.TextSecondary)
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
private fun NavRow(title: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        SliverText(title, SliverType.Body, Modifier.weight(1f))
        if (value != null) SliverText(value, SliverType.BodySmall, color = SliverColors.TextSecondary)
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary)
    }
}
