package app.sliver.feature.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sliver.core.APP_NAME
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.ReelogType
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.SwitchRow
import app.sliver.data.FilmFormat
import app.sliver.data.backup.backupRowValue
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.Appearance
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.camera.label
import app.sliver.feature.notifications.reminderSummary
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

internal const val FEEDBACK_ADDRESS = "feedback@sliver.app"
private val Formats = listOf(FilmFormat.CINEMA, FilmFormat.STORY)

/** Switch subtitles: they describe what the switch does in either state (audit v1.8 §1). */
object SettingsCaptions {
    const val DateStamp = "Burns the date into every new clip. A film can still hide it."
    const val FullRecordings = "The whole take goes to Photos \u203A Reelog, not just the 1 s clip."
    const val GoldenHour = "Shows when the evening light starts. Uses rough location."
    val all = listOf(DateStamp, FullRecordings, GoldenHour)
}

private val Appearances = listOf(Appearance.SYSTEM to "System", Appearance.LIGHT to "Light", Appearance.DARK to "Dark")

@Composable
fun SettingsScreen(onGo: (Route) -> Unit, onBack: () -> Unit) {
    val repo = remember { KoinPlatform.getKoin().get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val platform = rememberSettingsPlatform()
    val location = rememberPermissionController()
    val settings by remember { repo.settings }.collectAsStateWithLifecycle(null)
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

        // v1.8 SettingsV18: Account & backup / Camera / App. Subtitles say what a switch does, never one state (§1).
        Group("Account & backup") {
            NavRow(
                "Google account",
                if (s.account.signedIn) s.account.email?.substringBefore('@') ?: "Signed in" else "Not signed in",
                first = true
            ) { onGo(Route.Backup) }
            NavRow(
                "Backup & sync",
                backupRowValue(s.account.signedIn, s.account.autoBackup, s.account.lastSyncAt, kotlin.time.Clock.System.now().toEpochMilliseconds()),
            ) { onGo(Route.Backup) }
        }

        Group("Camera") {
            Hairline(first = true)
            SwitchRow(
                "Date stamp on clips",
                s.dateStamp,
                { on -> update { it.copy(dateStamp = on) } },
                Modifier.padding(horizontal = RowPad),
                caption = SettingsCaptions.DateStamp
            )
            LinedSwitch(
                "Keep full recordings in Photos",
                s.saveFullRecordings,
                { on -> update { it.copy(saveFullRecordings = on) } },
                SettingsCaptions.FullRecordings
            )
            // DECISION: the volume-key option lives in Settings (not on the Camera toolbar) and only acts while Camera is open.
            LinedSwitch("Record with volume keys", s.volumeKeyRecords, { on -> update { it.copy(volumeKeyRecords = on) } }, null)
            LinedSwitch(
                "Golden hour line",
                s.showGoldenHour,
                { on ->
                    update { it.copy(showGoldenHour = on) }
                    if (on && location.status(PermissionKind.CoarseLocation) == PermissionStatus.NotAsked) location.request(PermissionKind.CoarseLocation)
                },
                SettingsCaptions.GoldenHour,
            )
        }

        Group("App") {
            NavRow("Daily reminder", reminderSummary(s.reminder), first = true) { onGo(Route.Reminders) }
            ChoiceRow(
                title = "Appearance",
                options = Appearances.map { it.second },
                selected = Appearances.indexOfFirst { it.first == s.appearance },
            ) { i -> update { it.copy(appearance = Appearances[i].first) } }
            ChoiceRow(
                title = "Default film format",
                options = Formats.map { it.label() },
                selected = Formats.indexOf(s.newFilmFormat),
            ) { i -> update { it.copy(newFilmFormat = Formats[i]) } }
        }

        Group("About") {
            NavRow("What $APP_NAME stores", null, first = true) { showStores = !showStores }
            if (showStores) {
                SliverText(
                    "Your clips, photos and films stay on this phone. If you turn on backup, copies go to a private folder in your own Google Drive. Nothing is shared with us.",
                    SliverType.BodySmall,
                    Modifier.padding(horizontal = RowPad).padding(bottom = SliverSpace.S3),
                    color = SliverColors.TextSecondary,
                )
            }
            NavRow("Send feedback", null) { platform.sendFeedback(FEEDBACK_ADDRESS, "$APP_NAME feedback") }
        }

        SliverText(
            "$APP_NAME ${platform.versionName}",
            SliverType.Meta,
            Modifier.padding(top = SliverSpace.S6).combinedClickable(onClick = {}, onLongClick = { platform.seedDebugData() }),
            color = SliverColors.TextTertiary,
        )
    }
}

/** v1.7 SettingsV17: grouped card #141416, radius 18; rows 17 sp, values 15 sp #8E8E88, inset hairlines. */
@Composable
private fun Group(title: String, rows: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    SliverText(
        title,
        SliverType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        Modifier.padding(start = RowPad, top = SliverSpace.S6, bottom = SliverSpace.S2),
        color = SliverColors.TextTertiary,
    )
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(SliverColors.GroupCard), content = rows)
}

private val RowPad = 16.dp
private val GroupShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

@Composable
private fun Hairline(first: Boolean = false) {
    if (!first) Box(Modifier.padding(start = RowPad).fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
}

@Composable
private fun LinedSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit, caption: String?) {
    Hairline()
    SwitchRow(title, checked, onChange, Modifier.padding(horizontal = RowPad), caption = caption)
}

private val ValueStyle @Composable get() = ReelogType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal)

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, first: Boolean = false, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Hairline(first)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable {
            open = !open
        }.padding(horizontal = RowPad, vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        SliverText(title, SliverType.Body, Modifier.weight(1f))
        SliverText(options.getOrElse(selected) { "" }, ValueStyle, color = SliverColors.TextTertiary)
        SliverIcon(if (open) SliverIcons.ChevronDown else SliverIcons.ChevronRight, SliverColors.TextTertiary)
    }
    if (open) {
        options.forEachIndexed { i, label ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                    onSelect(i);
                    open = false
                }.padding(start = RowPad * 2, end = RowPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliverText(label, SliverType.Body, Modifier.weight(1f), if (i == selected) SliverColors.TextPrimary else SliverColors.TextSecondary)
                if (i == selected) SliverIcon(SliverIcons.Check, SliverColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun NavRow(title: String, value: String?, caption: String? = null, first: Boolean = false, onClick: () -> Unit) {
    Hairline(first)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = RowPad, vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Column(Modifier.weight(1f)) {
            SliverText(title, SliverType.Body)
            if (caption != null) SliverText(caption, SliverType.Caption)
        }
        if (value != null) SliverText(value, ValueStyle, color = SliverColors.TextTertiary)
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary)
    }
}
