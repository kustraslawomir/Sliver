package app.sliver.feature.films

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
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
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
import app.sliver.data.FilmRangeType
import app.sliver.data.LookPreset
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.mp.KoinPlatform

private val RangeTypes = listOf(FilmRangeType.WEEK, FilmRangeType.MONTH, FilmRangeType.YEAR, FilmRangeType.CUSTOM)
private val Audios = listOf(FilmAudio.ORIGINAL, FilmAudio.MUSIC, FilmAudio.MIX)

@Composable
fun FilmBuilderScreen(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }
    val all by remember { days.observeAll() }.collectAsState(emptyList())
    val app by remember { settings.settings }.collectAsState(null)

    var options by remember {
        mutableStateOf(FilmOptions(custom = today.plus(-13, DateTimeUnit.DAY) to today))
    }
    var dateTouched by remember { mutableStateOf(false) }
    val editId = remember { FilmNav.takeEditId() }
    var editing by remember { mutableStateOf<Film?>(null) }
    LaunchedEffect(editId) {
        val f = editId?.let { films.getFilm(it) } ?: return@LaunchedEffect
        editing = f
        options = FilmOptions(
            f.rangeType, (f.rangeFrom ?: today) to (f.rangeTo ?: today),
            if (isLegacyTrack(f.track)) FilmAudio.ORIGINAL else f.audio, usableTrack(f.track), f.dateOn, f.look,
        )
        dateTouched = true
    }
    var lowStorage by remember { mutableStateOf(false) }
    val dateOn = if (dateTouched) options.dateOn else app?.dateStamp ?: options.dateOn
    val format = editing?.format ?: app?.newFilmFormat ?: FilmFormat.CINEMA

    val range = rangeFor(options.rangeType, today, options.custom)
    val plan = remember(all, range) { buildFilmPlan(all, range.first, range.second) }
    val clips = remember(plan) { plan.days.map { FilmClip(it.file, it.look, it.keepAudio) } }
    val lookParams = options.look?.toParams()

    var paused by remember { mutableStateOf(false) }
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
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            SliverText(if (editId != null) "Edit film" else "New film", SliverType.Headline, Modifier.padding(start = SliverSpace.S2))
        }

        val ratio = if (format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f
        Box(
            Modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth(if (format == FilmFormat.CINEMA) 1f else 0.6f)
                .align(Alignment.CenterHorizontally).aspectRatio(ratio).clip(SliverShapes.M)
                .background(SliverColors.BgVoid).clickable { paused = !paused },
        ) {
            if (clips.isEmpty()) {
                SliverText("No clips in this range", SliverType.BodySmall, Modifier.align(Alignment.Center), SliverColors.OnMedia.copy(alpha = 0.7f))
            } else {
                FilmPreview(
                    clips, lookParams, options.audio, options.track, paused, seek,
                    onPosition = { i, f -> position = i to f }, modifier = Modifier.fillMaxSize(),
                )
                val index = position.first.coerceIn(0, clips.size - 1)
                if (dateOn) {
                    DateStamp(stampText(plan.days[index].date))
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
            SliverText("${plan.clips} clips · ${formatRuntime(plan.clips)}", SliverType.Meta, color = SliverColors.TextSecondary)
        }

        Section("Range") {
            Segmented(
                listOf("Week", "Month", "Year", "Custom"), RangeTypes.indexOf(options.rangeType),
                { options = options.copy(rangeType = RangeTypes[it]) },
            )
            if (options.rangeType == FilmRangeType.CUSTOM) {
                DateStepper("From", options.custom.first) {
                    options = options.copy(custom = it.coerceAtMost(options.custom.second) to options.custom.second)
                }
                DateStepper("To", options.custom.second) {
                    options = options.copy(custom = options.custom.first to it.coerceAtLeast(options.custom.first))
                }
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
            if (options.audio != FilmAudio.ORIGINAL) {
                val picked = trackLabel(options.track)
                if (picked != null) {
                    SliverText(picked, SliverType.BodySmall, color = SliverColors.AccentText, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
                    GhostButton(if (picked == null) "Pick a file" else "Replace", pickAudio)
                    if (picked != null) GhostButton("Remove", { setTrack(null) })
                }
                notice?.let { SliverText(it, SliverType.Caption, color = SliverColors.Error) }
                if (picked == null) SliverText(PICK_MUSIC_HINT, SliverType.Caption, color = SliverColors.TextSecondary)
            }
        }

        Column(Modifier.padding(horizontal = SliverSpace.Gutter)) {
            SwitchRow("Date on screen", dateOn, { dateTouched = true; options = options.copy(dateOn = it) })
        }

        if (lowStorage) {
            SliverText(
                "$STORAGE_FULL_TITLE. $STORAGE_FULL_BODY", SliverType.Caption,
                Modifier.padding(horizontal = SliverSpace.Gutter), SliverColors.Error,
            )
        }
        PrimaryButton(
            "Save film",
            {
                lowStorage = isStorageLow(koin.get<FileStore>().freeSpaceBytes())
                if (!lowStorage) scope.launch {
                    val old = editing
                    val id = old?.id ?: "film-${Clock.System.now().toEpochMilliseconds()}"
                    val film = options.copy(dateOn = dateOn).toFilm(id, old?.name ?: filmName(options, range), old?.format ?: format, today)
                        .let { if (old != null) it.copy(projectId = old.projectId) else it }
                    films.save(film)
                    koin.get<FilmRenderer>().start(id)
                    onSaved(id)
                }
            },
            Modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth(),
            enabled = clips.isNotEmpty() && canSaveSound(options.audio, options.track),
        )
    }
}

private fun filmName(options: FilmOptions, range: Pair<LocalDate, LocalDate>): String = when (options.rangeType) {
    FilmRangeType.WEEK -> "Last 7 days"
    FilmRangeType.MONTH -> MonthLong[range.first.monthNumber - 1]
    FilmRangeType.YEAR -> "${range.first.year}"
    FilmRangeType.CUSTOM -> "${range.first.dayOfMonth} ${MonthLong[range.first.monthNumber - 1].take(3)} – " +
        "${range.second.dayOfMonth} ${MonthLong[range.second.monthNumber - 1].take(3)}"
}

private val MonthLong = listOf(
    "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November",
    "December",
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText(title, SliverType.Meta, color = SliverColors.TextSecondary)
        content()
    }
}

@Composable
private fun DateStepper(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SliverText(label, SliverType.BodySmall, Modifier.weight(1f), SliverColors.TextSecondary)
        GhostButton("-7", { onChange(date.plus(-7, DateTimeUnit.DAY)) })
        GhostButton("-1", { onChange(date.plus(-1, DateTimeUnit.DAY)) })
        SliverText(stampText(date), SliverType.Stamp, Modifier.padding(horizontal = SliverSpace.S2))
        GhostButton("+1", { onChange(date.plus(1, DateTimeUnit.DAY)) })
        GhostButton("+7", { onChange(date.plus(7, DateTimeUnit.DAY)) })
    }
}
