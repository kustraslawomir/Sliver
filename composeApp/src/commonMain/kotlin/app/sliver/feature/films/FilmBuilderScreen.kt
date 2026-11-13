package app.sliver.feature.films

import app.sliver.core.theme.ReelogType

import app.sliver.core.ui.SliverBottomSheet
import androidx.compose.foundation.layout.heightIn
import app.sliver.core.ui.Chip
import app.sliver.core.ui.OnAppHidden
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SliverSlider
import app.sliver.core.ui.STORAGE_FULL_BODY
import app.sliver.core.ui.STORAGE_FULL_TITLE
import app.sliver.core.ui.SwitchRow
import app.sliver.core.ui.isStorageLow
import app.sliver.data.files.FileStore
import app.sliver.data.FilmAudio
import app.sliver.data.FilmFormat
import app.sliver.data.StampStyle
import app.sliver.feature.trim.rememberPhoto
import app.sliver.core.ui.DateStamp
import app.sliver.data.export.stampText
import app.sliver.data.info
import app.sliver.data.db.Film
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.data.toParams
import kotlin.time.Clock
import kotlinx.coroutines.launch
import app.sliver.data.repo.ProjectRepository
import org.koin.mp.KoinPlatform

private val Audios = listOf(FilmAudio.ORIGINAL, FilmAudio.MUSIC, FilmAudio.MIX)

@Composable
fun FilmBuilderScreen(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)
    val project by remember { koin.get<ProjectRepository>().active }.collectAsState(null)

    var options by remember { mutableStateOf(FilmOptions()) }
    var dateTouched by remember { mutableStateOf(false) }
    val editId = remember { FilmNav.takeEditId() }
    var editing by remember { mutableStateOf<Film?>(null) }
    LaunchedEffect(editId) {
        val f = editId?.let { films.getFilm(it) } ?: return@LaunchedEffect
        editing = f
        options = FilmOptions(
            if (isLegacyTrack(f.track)) FilmAudio.ORIGINAL else f.audio, usableTrack(f.track), f.dateOn, f.look, f.lookIntensity, f.lookGrain, f.stampStyle, f.musicVolume,
        )
        dateTouched = true
    }
    var lowStorage by remember { mutableStateOf(false) }
    val dateOn = if (dateTouched) options.dateOn else app?.dateStamp ?: options.dateOn
    val format = editing?.format ?: app?.newFilmFormat ?: FilmFormat.CINEMA

    val plan = remember(all) { buildFilmPlan(all) }
    val clips = remember(plan) { plan.days.map { FilmClip(it.file, it.keepAudio) } }
    val lookParams = options.look?.toParams(options.intensity, options.grain)

    var paused by remember { mutableStateOf(FilmPlayback.paused) }
    LaunchedEffect(paused) { FilmPlayback.paused = paused }
    OnAppHidden { paused = true }
    var seek by remember { mutableStateOf<FilmSeek?>(null) }
    var position by remember { mutableStateOf(0 to 0f) }
    val fileStore = remember { koin.get<FileStore>() }
    var notice by remember { mutableStateOf<String?>(null) }
    // Owner: imported music is kept in the app (music library), so switching tracks never deletes a file.
    fun setTrack(path: String?) {
        options = options.copy(track = path)
    }
    val pickAudio = rememberAudioPicker {
        when (it) {
            is PickedAudio.Ok -> { notice = null; setTrack(it.path) }
            is PickedAudio.Failed -> notice = it.message
        }
    }

    var adjusting by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
    // Owner: the whole screen scrolls (preview, timeline and filters stay first).
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SliverText(editing?.name ?: project?.name ?: "New film", SliverType.Headline.copy(fontSize = ReelogType.Size.body, lineHeight = ReelogType.lineFor(ReelogType.Size.body)), maxLines = 1)
                SliverText(
                    if (format == FilmFormat.CINEMA) "Cinema 16:9" else "Story 9:16",
                    SliverType.BodySmall.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption)), color = SliverColors.TextTertiary,
                )
            }
            Spacer(Modifier.size(44.dp))
        }

        // Owner: video, timeline and filters stay on screen together (no scrolling away from the preview).
        val ratio = if (format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f
        Box(
            Modifier.fillMaxWidth().heightIn(max = PreviewMaxHeight).padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S1),
            Alignment.Center,
        ) {
            Box(
                Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f).clip(SliverShapes.M)
                    .background(SliverColors.BgVoid).clickable { paused = !paused },
            ) {
                if (clips.isEmpty()) {
                    SliverText("No clips yet", SliverType.BodySmall, Modifier.align(Alignment.Center), SliverColors.OnMedia.copy(alpha = 0.7f))
                } else {
                    FilmPreview(
                        clips, lookParams, options.audio, options.track, paused, seek,
                        onPosition = { i, f -> position = i to f }, modifier = Modifier.fillMaxSize(),
                        musicVolume = options.musicVolume,
                    )
                    val index = position.first.coerceIn(0, clips.size - 1)
                    if (dateOn) {
                        val style = options.stamp ?: options.look?.info?.stamp ?: StampStyle.PLAIN
                        DateStamp(stampText(plan.days[index].date, style), style = style)
                    }
                    if (paused) SliverText("Paused", SliverType.Meta, Modifier.align(Alignment.Center), SliverColors.OnMedia)
                    IconButton44(
                        { FilmPlayback.muted = !FilmPlayback.muted }, if (FilmPlayback.muted) "Unmute" else "Mute",
                        Modifier.align(Alignment.BottomStart).padding(SliverSpace.S1), overCamera = true,
                    ) { SliverIcon(if (FilmPlayback.muted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.OnMedia) }
                }
            }
        }

        Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
            val fraction = if (clips.isEmpty()) 0f else ((position.first + position.second) / clips.size).coerceIn(0f, 1f)
            val ticks = remember(plan) { scrubberTicks(plan) }
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                ticks.forEach {
                    val x = it * size.width
                    drawLine(SliverColors.LineStrong, Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
            SliverSlider(fraction, { seek = FilmSeek(it) })
            Row(Modifier.fillMaxWidth()) {
                val at = (fraction * plan.clips).toInt()
                SliverText("${clockTime(at)} / ${clockTime(plan.clips)}", SliverType.Meta.copy(fontSize = ReelogType.Size.caption), Modifier.weight(1f), SliverColors.TextTertiary)
                SliverText("${clipsText(plan.clips)} \u00B7 ${plan.spanText()}", SliverType.Meta.copy(fontSize = ReelogType.Size.caption), color = SliverColors.TextTertiary, maxLines = 1)
            }
        }

        val frame = plan.days.getOrNull(plan.days.size / 2)?.let { rememberPhoto(it.thumb) }
        FilmFilterSection(
            frame, options.look, options.intensity, options.grain,
            onSelect = { options = options.copy(look = it, grain = null) },
            onIntensity = { options = options.copy(intensity = it) },
            onGrain = { options = options.copy(grain = it) },
            modifier = Modifier.padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2),
            onAdjust = { adjusting = true },
        )

        Column(
            Modifier.padding(top = SliverSpace.S2, bottom = SliverSpace.S6),
            verticalArrangement = Arrangement.spacedBy(SliverSpace.S3),
        ) {
            Section("Sound") {
                Segmented(
                    listOf("Original", "Music", "Mix"), Audios.indexOf(options.audio),
                    { options = options.copy(audio = Audios[it]) }, compact = true,
                )
                if (options.track == null && isLegacyTrack(editing?.track)) {
                    SliverText(LEGACY_TRACK_NOTE, SliverType.Caption, color = SliverColors.TextSecondary)
                }
                if (options.audio != FilmAudio.ORIGINAL) {
                    // Owner: music loaded once stays in the app; pick from the list, add new files only when needed.
                    val library = rememberMusicLibrary(options.track)
                    library.forEach { path ->
                        val on = path == options.track
                        Row(
                            Modifier.fillMaxWidth().height(44.dp).clip(SliverShapes.R12)
                                .background(if (on) SliverColors.Surface2 else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { setTrack(path) }.padding(horizontal = SliverSpace.S3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
                        ) {
                            SliverIcon(if (on) SliverIcons.Check else SliverIcons.Volume2, if (on) SliverColors.TextPrimary else SliverColors.TextTertiary, size = 18.dp)
                            SliverText(trackLabel(path) ?: path.substringAfterLast('/'), SliverType.BodySmall, Modifier.weight(1f), color = if (on) SliverColors.TextPrimary else SliverColors.TextSecondary, maxLines = 1)
                        }
                    }
                    if (library.isEmpty()) SliverText(PICK_MUSIC_HINT, SliverType.BodySmall, color = SliverColors.TextTertiary)
                    GhostButton("Add an audio file\u2026", pickAudio)
                    notice?.let { SliverText(it, SliverType.Caption, color = SliverColors.Error) }
                    if (options.track != null) {
                        val vol = musicVolumePercent(options.audio, options.musicVolume)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SliverText("Music volume", SliverType.BodySmall, Modifier.weight(1f), color = SliverColors.TextSecondary)
                            SliverText("$vol", SliverType.Meta, color = SliverColors.TextSecondary)
                        }
                        SliverSlider(vol / 100f, { options = options.copy(musicVolume = (it * 100).toInt().coerceIn(0, 100)) })
                    }
                }
            }

            Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
                SwitchRow("Date on screen", dateOn, { dateTouched = true; options = options.copy(dateOn = it) })
                if (dateOn) {
                    // Owner: pick the date style; same compact control as Sound.
                    val current = options.stamp ?: options.look?.info?.stamp ?: StampStyle.PLAIN
                    Segmented(
                        StampChoices.map { it.second }, StampChoices.indexOfFirst { it.first == current }.coerceAtLeast(0),
                        { options = options.copy(stamp = StampChoices[it].first) }, compact = true,
                    )
                }
            }

            if (lowStorage) {
                SliverText(
                    "$STORAGE_FULL_TITLE. $STORAGE_FULL_BODY", SliverType.Caption,
                    Modifier.padding(horizontal = SliverSpace.Gutter), SliverColors.Error,
                )
            }
            PrimaryButton(
                "${if (editId != null) "Save film" else "Make film"} \u00B7 ${clockTime(plan.clips)}",
                {
                    lowStorage = isStorageLow(koin.get<FileStore>().freeSpaceBytes())
                    if (!lowStorage) scope.launch {
                        val old = editing
                        val id = old?.id ?: "film-${Clock.System.now().toEpochMilliseconds()}"
                        val film = options.copy(dateOn = dateOn).toFilm(id, old?.name ?: project?.name ?: "My film", old?.format ?: format)
                            .let { if (old != null) it.copy(projectId = old.projectId) else it }
                        films.save(film)
                        koin.get<FilmRenderer>().start(id)
                        onSaved(id)
                    }
                },
                Modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth(),
                enabled = clips.isNotEmpty() && canSaveSound(options.audio, options.track),
            )
            SliverText(
                "Rendered on your phone, 1080p. Works offline.", SliverType.Caption.copy(textAlign = TextAlign.Center),
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), SliverColors.TextTertiary,
            )
        }
    }
    val adjustLook = options.look
    SliverBottomSheet(adjusting && adjustLook != null, onDismiss = { adjusting = false }) {
        if (adjustLook != null) {
            FilterAmounts(
                adjustLook, options.intensity, options.grain,
                onIntensity = { options = options.copy(intensity = it) },
                onGrain = { options = options.copy(grain = it) },
            )
        }
    }
    }
}

private val PreviewMaxHeight = 300.dp

private val StampChoices = listOf(StampStyle.LED to "Film", StampStyle.PLAIN to "Plain", StampStyle.CAMCORDER to "Camcorder")

/** "02:09" */
private fun clockTime(seconds: Int) = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(title, SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), color = SliverColors.TextTertiary)
        content()
    }
}
