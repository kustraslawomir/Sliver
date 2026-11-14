package app.sliver.feature.films

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import app.sliver.core.kit.GlassStyle
import app.sliver.core.kit.glass
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.gestures.detectTapGestures

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
    app.sliver.core.log.RecomposeProbe("builder")
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val all by remember { days.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val app by remember { settings.settings }.collectAsStateWithLifecycle(null)
    val project by remember { koin.get<ProjectRepository>().active }.collectAsStateWithLifecycle(null)

    // Unsaved choices survive recreation (theme change, rotation); the film being edited is loaded once.
    var options by rememberSaveable(stateSaver = FilmOptionsSaver) { mutableStateOf(FilmOptions()) }
    var dateTouched by rememberSaveable { mutableStateOf(false) }
    val editId: String? = rememberSaveable { mutableStateOf(FilmNav.takeEditId()) }.value
    var editing by remember { mutableStateOf<Film?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editId) {
        val f = editId?.let { films.getFilm(it) } ?: return@LaunchedEffect
        editing = f
        if (loaded) return@LaunchedEffect
        loaded = true
        val loadedMix = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { f.audioMix(::audioDurationMs) }
        options = FilmOptions(
            if (isLegacyTrack(f.track)) FilmAudio.ORIGINAL else f.audio, usableTrack(f.track), f.dateOn, f.look, f.lookIntensity, f.lookGrain, f.stampStyle, f.musicVolume,
            mix = loadedMix,
        )
        dateTouched = true
    }
    var lowStorage by remember { mutableStateOf(false) }
    val dateOn = if (dateTouched) options.dateOn else app?.dateStamp ?: options.dateOn
    val format = editing?.format ?: app?.newFilmFormat ?: FilmFormat.CINEMA

    val plan = remember(all) { buildFilmPlan(all) }
    val clips = remember(plan) { plan.days.map { FilmClip(it.file, it.keepAudio, it.date.toString()) } }
    val mixClips = remember(plan) { plan.days.map { MixClip(it.date, it.keepAudio, it.thumb, it.color) } }
    val filmMs = filmLengthMs(clips.size)
    val mix = options.mix ?: AudioMix()
    var addingLayer by rememberSaveable { mutableStateOf(false) }
    var soundOpen by rememberSaveable { mutableStateOf(false) }
    var barHeight by remember { mutableStateOf(120.dp) }
    var mutedOpen by rememberSaveable { mutableStateOf(false) }
    var dateSheet by rememberSaveable { mutableStateOf(false) }
    var editingLayer by rememberSaveable { mutableStateOf<Int?>(null) }
    var libraryRefresh by remember { mutableStateOf(0) }
    val lookParams = options.look?.toParams(options.intensity, options.grain)

    var paused by remember { mutableStateOf(FilmPlayback.paused) }
    LaunchedEffect(paused) { FilmPlayback.paused = paused }
    OnAppHidden { paused = true }
    var seek by remember { mutableStateOf<FilmSeek?>(null) }
    var comparing by remember { mutableStateOf(false) }
    // Perf STEP 2: the preview reports its position ~30×/s. The body never reads it directly — only the clip index and
    // whole seconds (derived, change once a second) and the draw lambdas below.
    val positionState = remember { mutableStateOf(0 to 0f) }
    val clipIndex by remember { derivedStateOf { positionState.value.first } }
    val atSecond by remember { derivedStateOf { positionState.value.let { (it.first + it.second).toInt() } } }
    fun playheadMs(): Long = positionState.value.let { ((it.first + it.second) * FILM_FRAME_MS).toLong() }
    val fileStore = remember { koin.get<FileStore>() }
    var notice by remember { mutableStateOf<String?>(null) }
    // Owner 14 Nov: a new layer starts at the playhead (the first one at the start); imported files stay in the library.
    fun addLayer(path: String) {
        val m = options.mix ?: AudioMix()
        val start = if (m.layers.isEmpty()) 0L else playheadMs()
        options = options.copy(mix = m.copy(layers = m.layers + AudioLayer(path, audioDurationMs(path), startMs = start)))
    }
    val pickAudio = rememberAudioPicker {
        when (it) {
            is PickedAudio.Ok -> { notice = null; libraryRefresh++; addLayer(it.path) }
            is PickedAudio.Failed -> notice = it.message
        }
    }

    var adjusting by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
    // Owner: the whole screen scrolls (preview, timeline and filters stay first); Make film is a sticky bar (BuilderV18).
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = barHeight + 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton44(onBack, "Back") { SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SliverText(editing?.name ?: project?.name ?: "New film", SliverType.Headline.copy(fontSize = ReelogType.Size.body, lineHeight = ReelogType.lineFor(ReelogType.Size.body)), maxLines = 1)
                // BuilderV18: "Story 9:16 · 1 Apr – 13 Nov · 155 clips".
                SliverText(
                    listOfNotNull(if (format == FilmFormat.CINEMA) "Cinema 16:9" else "Story 9:16", plan.spanText().takeIf { plan.clips > 0 }, clipsText(plan.clips)).joinToString(" \u00B7 "),
                    SliverType.BodySmall.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption)), color = SliverColors.TextTertiary, maxLines = 1,
                )
            }
            Spacer(Modifier.size(44.dp))
        }

        // Owner: video, timeline and filters stay on screen together (no scrolling away from the preview).
        val ratio = if (format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f
        // BuilderV18: 9:16 = 164 × 290 dp; 16:9 = full width.
        Box(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2),
            Alignment.Center,
        ) {
            Box(
                (if (ratio < 1f) Modifier.size(164.dp, 290.dp) else Modifier.fillMaxWidth().aspectRatio(ratio)).clip(SliverShapes.R20)
                    .background(SliverColors.BgVoid)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { paused = !paused },
                            onLongPress = { comparing = true },
                            onPress = { tryAwaitRelease(); comparing = false },
                        )
                    },
            ) {
                if (clips.isEmpty()) {
                    SliverText("No clips yet", SliverType.BodySmall, Modifier.align(Alignment.Center), SliverColors.OnMedia.copy(alpha = 0.7f))
                } else {
                    FilmPreview(
                        clips, lookParams, mix, paused, seek,
                        onPosition = { i, f -> positionState.value = i to f }, modifier = Modifier.fillMaxSize(),
                        compare = comparing,
                    )
                    if (comparing && lookParams != null) {
                        SliverText(
                            "Original", SliverType.Meta, Modifier.align(Alignment.TopCenter).padding(top = SliverSpace.S2)
                                .background(SliverColors.BgVoid.copy(alpha = 0.55f), SliverShapes.Pill).padding(horizontal = SliverSpace.S2, vertical = 4.dp),
                            SliverColors.OnMedia,
                        )
                    }
                    val index = clipIndex.coerceIn(0, clips.size - 1)
                    if (dateOn) {
                        val style = options.stamp ?: options.look?.info?.stamp ?: StampStyle.PLAIN
                        DateStamp(stampText(plan.days[index].date, style), style = style)
                    }
                    // Audit v1.8 §4.2: a 56 dp glass play circle instead of the word "Paused"; it fades 600 ms after play.
                    val playAlpha by androidx.compose.animation.core.animateFloatAsState(
                        if (paused && !comparing) 1f else 0f,
                        androidx.compose.animation.core.tween(if (paused) 150 else 300, delayMillis = if (paused) 0 else 600), label = "play",
                    )
                    // v1.9 §4.1: a filled 56 dp circle (white 85 %, dark icon) — the outline one vanished on bright frames.
                    if (playAlpha > 0f) Box(
                        Modifier.align(Alignment.Center).size(56.dp).alpha(playAlpha)
                            .clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.85f)),
                        Alignment.Center,
                    ) { SliverIcon(if (paused) SliverIcons.Play else SliverIcons.Pause, SliverColors.Media, size = 24.dp) }
                    IconButton44(
                        { FilmPlayback.muted = !FilmPlayback.muted }, if (FilmPlayback.muted) "Unmute" else "Mute",
                        Modifier.align(Alignment.BottomStart).padding(SliverSpace.S1), overCamera = true,
                    ) { SliverIcon(if (FilmPlayback.muted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.OnMedia) }
                }
            }
        }

        Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
            // BuilderV18: a 36 dp filmstrip of real thumbnails with a 3 dp white playhead (replaces the line + ticks).
            // The playhead is read while drawing, so playback redraws the strip instead of recomposing the screen.
            Filmstrip(plan, { if (clips.isEmpty()) 0f else positionState.value.let { (it.first + it.second) / clips.size }.coerceIn(0f, 1f) }) { seek = FilmSeek(it) }
            Row(Modifier.fillMaxWidth().padding(top = SliverSpace.S2), horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
                SliverText("${formatRuntime(atSecond.coerceIn(0, plan.clips))} / ${filmRuntime(plan.clips)}", SliverType.Meta.copy(fontSize = ReelogType.Size.caption), color = SliverColors.TextTertiary, maxLines = 1)
                SliverText(
                    stripMonths(plan), SliverType.Meta.copy(fontSize = ReelogType.Size.caption, textAlign = TextAlign.End), Modifier.weight(1f), SliverColors.TextTertiary,
                    maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }

        // v1.9 §4.3: the Look tiles show the frame under the playhead (your film), refreshed when paused or seeking.
        var frameIndex by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(clipIndex, paused) { if (paused) frameIndex = clipIndex }
        LaunchedEffect(seek) { if (seek != null) { kotlinx.coroutines.delay(400); frameIndex = positionState.value.first } }
        val frame = plan.days.getOrNull(frameIndex.coerceIn(0, (plan.days.size - 1).coerceAtLeast(0)))?.let { rememberPhoto(it.thumb) }
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
            // BuilderV18: Sound and Date on screen are rows with a value; the detail lives in sheets.
            Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
                val (soundValue, soundSub) = soundSummary(mix)
                ValueRow("Sound", soundSub, soundValue) { soundOpen = true }
                if (mix.layers.isEmpty() && isLegacyTrack(editing?.track)) {
                    SliverText(LEGACY_TRACK_NOTE, SliverType.Caption, color = SliverColors.TextSecondary)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(SliverColors.LineHairline))
                val current = options.stamp ?: options.look?.info?.stamp ?: StampStyle.PLAIN
                ValueRow("Date on screen", null, if (dateOn) StampChoices.first { it.first == current }.second else "Off") { dateSheet = true }
            }

        }
    }
    // BuilderV18 / audit §4.6: sticky Make film over a fade; never cut by the nav bar.
    // v1.9 §4.2: the scroll content ends 16 dp above the bar (measured), with a 24 dp fade above it.
    val density = androidx.compose.ui.platform.LocalDensity.current
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
            .background(Brush.verticalGradient(0f to SliverColors.BgBase.copy(alpha = 0f), 0.18f to SliverColors.BgBase))
            .navigationBarsPadding().padding(top = 24.dp, bottom = SliverSpace.S3),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S2),
    ) {
            if (lowStorage) {
            SliverText(
                "$STORAGE_FULL_TITLE. $STORAGE_FULL_BODY", SliverType.Caption,
                Modifier.padding(horizontal = SliverSpace.Gutter), SliverColors.Error,
            )
        }
        PrimaryButton(
            "${if (editId != null) "Save film" else "Make film"} \u00B7 ${filmRuntime(plan.clips)}",
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
            enabled = clips.isNotEmpty(),
        )
        SliverText(
            "Rendered on your phone, 1080p. Works offline.", SliverType.Caption.copy(textAlign = TextAlign.Center),
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), SliverColors.TextTertiary,
        )
    }
    val adjustLook = options.look
    SliverBottomSheet(mutedOpen, onDismiss = { mutedOpen = false }) {
        MutedClipsGrid(mix, { options = options.copy(mix = it) }, mixClips, onDone = { mutedOpen = false })
    }
    SliverBottomSheet(soundOpen && !addingLayer && editingLayer == null && !mutedOpen, onDismiss = { soundOpen = false }) {
        Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
            AudioMixSection(
                mix, { options = options.copy(mix = it) },
                mixClips, filmMs, ::playheadMs,
                onAddLayer = { addingLayer = true },
                onEditLayer = { editingLayer = it },
                onMutedClips = { mutedOpen = true },
            )
            PrimaryButton("Done", { soundOpen = false }, Modifier.fillMaxWidth().padding(top = SliverSpace.S4))
        }
    }
    SliverBottomSheet(addingLayer, onDismiss = { addingLayer = false }) {
        val library = rememberMusicLibrary(libraryRefresh)
        MusicList(
            library, ::audioDurationMs, filmMs, mix, { options = options.copy(mix = it) }, notice,
            onImport = pickAudio, onDone = { addingLayer = false }, onRemoved = { libraryRefresh++ },
        )
    }
    SliverBottomSheet(dateSheet, onDismiss = { dateSheet = false }) {
        val current = options.stamp ?: options.look?.info?.stamp ?: StampStyle.PLAIN
        Column {
            SliverText("Date on screen", SliverType.Title, Modifier.padding(bottom = SliverSpace.S2))
            DateChoice("Off", !dateOn, null) { dateTouched = true; options = options.copy(dateOn = false); dateSheet = false }
            StampChoices.forEach { (style, label) ->
                DateChoice(label, dateOn && current == style, style) {
                    dateTouched = true; options = options.copy(dateOn = true, stamp = style); dateSheet = false
                }
            }
        }
    }
    val layerIndex = editingLayer
    val editedLayer = layerIndex?.let { mix.layers.getOrNull(it) }
    SliverBottomSheet(editedLayer != null, onDismiss = { editingLayer = null }) {
        if (editedLayer != null && layerIndex != null) {
            LayerEditor(
                editedLayer, filmMs, ::playheadMs,
                onChange = { l -> options = options.copy(mix = mix.copy(layers = mix.layers.toMutableList().also { it[layerIndex] = l })) },
                onRemove = {
                    options = options.copy(mix = mix.copy(layers = mix.layers.filterIndexed { i, _ -> i != layerIndex }))
                    editingLayer = null
                },
            )
        }
    }
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

private val SampleDate = kotlinx.datetime.LocalDate(2026, 4, 1)

/** BuilderV18 row: title (+ sub line), value and chevron; opens a sheet. */
@Composable
private fun ValueRow(title: String, sub: String?, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(vertical = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SliverSpace.S3),
    ) {
        Column(Modifier.weight(1f)) {
            SliverText(title, SliverType.Body)
            sub?.let { SliverText(it, SliverType.BodySmall, color = SliverColors.TextTertiary, maxLines = 1) }
        }
        SliverText(value, SliverType.Body, color = SliverColors.TextSecondary, maxLines = 1)
        SliverIcon(SliverIcons.ChevronRight, SliverColors.TextTertiary, size = 18.dp)
    }
}

@Composable
private fun DateChoice(label: String, on: Boolean, sample: StampStyle?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).clip(SliverShapes.R12).clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .padding(horizontal = SliverSpace.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SliverText(label, SliverType.Body, Modifier.weight(1f))
        // v1.9 §4.5: a small sample of the style (European date order, as the owner asked).
        if (sample != null) {
            Box(Modifier.padding(end = SliverSpace.S3).size(150.dp, 44.dp).clip(SliverShapes.R12).background(SliverColors.BgVoid), Alignment.Center) {
                when (sample) {
                    StampStyle.LED -> DateStamp(stampText(SampleDate, sample), style = sample)
                    StampStyle.CAMCORDER -> Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.size(5.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SliverColors.Record))
                            SliverText("REC", SliverType.Stamp, color = Color.White)
                        }
                        SliverText(stampText(SampleDate, sample), SliverType.Stamp, color = Color.White, maxLines = 1)
                    }
                    else -> SliverText(stampText(SampleDate, sample), SliverType.Stamp, color = Color.White, maxLines = 1)
                }
            }
        }
        Box(Modifier.size(20.dp)) { if (on) SliverIcon(SliverIcons.Check, SliverColors.TextPrimary, size = 20.dp) }
    }
}


private val StampChoices = listOf(StampStyle.PLAIN to "Plain", StampStyle.LED to "Film", StampStyle.CAMCORDER to "Camcorder")

/** "02:09" */
private fun clockTime(seconds: Int) = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(title, SliverType.BodySmall.copy(fontSize = ReelogType.Size.label, lineHeight = ReelogType.lineFor(ReelogType.Size.label)), color = SliverColors.TextTertiary)
        content()
    }
}
