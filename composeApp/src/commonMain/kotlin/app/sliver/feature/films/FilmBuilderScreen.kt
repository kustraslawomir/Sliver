package app.sliver.feature.films

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
            if (isLegacyTrack(f.track)) FilmAudio.ORIGINAL else f.audio, usableTrack(f.track), f.dateOn, f.look, f.lookIntensity, f.lookGrain,
        )
        dateTouched = true
    }
    var lowStorage by remember { mutableStateOf(false) }
    val dateOn = if (dateTouched) options.dateOn else app?.dateStamp ?: options.dateOn
    val format = editing?.format ?: app?.newFilmFormat ?: FilmFormat.CINEMA

    val plan = remember(all) { buildFilmPlan(all) }
    val clips = remember(plan) { plan.days.map { FilmClip(it.file, it.keepAudio) } }
    val lookParams = options.look?.toParams(options.intensity, options.grain)

    var paused by remember { mutableStateOf(false) }
    OnAppHidden { paused = true }
    var seek by remember { mutableStateOf<FilmSeek?>(null) }
    var position by remember { mutableStateOf(0 to 0f) }
    val fileStore = remember { koin.get<FileStore>() }
    var notice by remember { mutableStateOf<String?>(null) }
    val fresh = remember { mutableSetOf<String>() }
    fun setTrack(path: String?) {
        options.track?.takeIf { it in fresh }?.let { fileStore.delete(it); fresh -= it }
        if (path != null) fresh += path
        options = options.copy(track = path)
    }
    val pickAudio = rememberAudioPicker {
        when (it) {
            is PickedAudio.Ok -> { notice = null; setTrack(it.path) }
            is PickedAudio.Failed -> notice = it.message
        }
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(bottom = SliverSpace.S8),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SliverText(editing?.name ?: project?.name ?: "New film", SliverType.Headline.copy(fontSize = 17.sp, lineHeight = 22.sp), maxLines = 1)
                SliverText(
                    if (format == FilmFormat.CINEMA) "Cinema 16:9" else "Story 9:16",
                    SliverType.BodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = SliverColors.TextTertiary,
                )
            }
            Spacer(Modifier.size(44.dp))
        }

        val ratio = if (format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f
        Box(
            Modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth(if (format == FilmFormat.CINEMA) 1f else 0.6f)
                .align(Alignment.CenterHorizontally).aspectRatio(ratio).clip(SliverShapes.M)
                .background(SliverColors.BgVoid).clickable { paused = !paused },
        ) {
            if (clips.isEmpty()) {
                SliverText("No clips yet", SliverType.BodySmall, Modifier.align(Alignment.Center), SliverColors.OnMedia.copy(alpha = 0.7f))
            } else {
                FilmPreview(
                    clips, lookParams, options.audio, options.track, paused, seek,
                    onPosition = { i, f -> position = i to f }, modifier = Modifier.fillMaxSize(),
                )
                val index = position.first.coerceIn(0, clips.size - 1)
                if (dateOn) {
                    val style = options.look?.info?.stamp ?: StampStyle.PLAIN
                    DateStamp(stampText(plan.days[index].date, style), style = style)
                }
                if (paused) SliverText("Paused", SliverType.Meta, Modifier.align(Alignment.Center), SliverColors.OnMedia)
            }
        }

        Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
            val fraction = if (clips.isEmpty()) 0f else ((position.first + position.second) / clips.size).coerceIn(0f, 1f)
            val ticks = remember(plan) { scrubberTicks(plan) }
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                ticks.forEach {
                    val x = it * size.width
                    drawLine(SliverColors.LineStrong, Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
            SliverSlider(fraction, { seek = FilmSeek(it) })
            Row(Modifier.fillMaxWidth()) {
                val at = (fraction * plan.clips).toInt()
                SliverText("${clockTime(at)} / ${clockTime(plan.clips)}", SliverType.Meta.copy(fontSize = 13.sp), Modifier.weight(1f), SliverColors.TextTertiary)
                SliverText("${plan.clips} clips \u00B7 ${plan.spanText()}", SliverType.Meta.copy(fontSize = 13.sp), color = SliverColors.TextTertiary, maxLines = 1)
            }
        }

        Section("Sound") {
            Segmented(
                listOf("Original", "Music", "Mix"), Audios.indexOf(options.audio),
                { options = options.copy(audio = Audios[it]) },
            )
            if (options.track == null && isLegacyTrack(editing?.track)) {
                SliverText(LEGACY_TRACK_NOTE, SliverType.Caption, color = SliverColors.TextSecondary)
            }
            if (options.audio == FilmAudio.ORIGINAL) {
                SliverText("Every clip keeps its own sound.", SliverType.BodySmall, color = SliverColors.TextTertiary)
            } else {
                // Board FilmBuilder: no bundled tracks, only the user's own file.
                val picked = trackLabel(options.track)
                if (picked != null) SliverText(picked, SliverType.BodySmall, color = SliverColors.TextPrimary, maxLines = 1)
                else SliverText(PICK_MUSIC_HINT, SliverType.BodySmall, color = SliverColors.TextTertiary)
                GhostButton(if (picked == null) "Pick an audio file\u2026" else "Pick another audio file\u2026", pickAudio)
                notice?.let { SliverText(it, SliverType.Caption, color = SliverColors.Error) }
            }
        }

        Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
            SwitchRow("Date on screen", dateOn, { dateTouched = true; options = options.copy(dateOn = it) })
            Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
        }

        val frame = plan.days.getOrNull(plan.days.size / 2)?.let { rememberPhoto(it.thumb) }
        FilmFilterSection(
            frame, options.look, options.intensity, options.grain,
            onSelect = { options = options.copy(look = it, grain = null) },
            onIntensity = { options = options.copy(intensity = it) },
            onGrain = { options = options.copy(grain = it) },
            modifier = Modifier.padding(horizontal = SliverSpace.Gutter),
        )


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

/** "02:09" */
private fun clockTime(seconds: Int) = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(title, SliverType.BodySmall.copy(fontSize = 14.sp, lineHeight = 18.sp), color = SliverColors.TextTertiary)
        content()
    }
}
