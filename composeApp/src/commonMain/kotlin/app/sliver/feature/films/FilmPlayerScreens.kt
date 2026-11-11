package app.sliver.feature.films

import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.LocalReducedMotion
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.BackHandler
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.SliverBottomSheet
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.PrimaryButton
import app.sliver.core.ui.SecondaryButton
import app.sliver.core.ui.STORAGE_FULL_BODY
import app.sliver.core.ui.STORAGE_FULL_TITLE
import app.sliver.core.ui.isStorageLow
import app.sliver.data.files.FileStore
import app.sliver.core.ui.SliverSlider
import app.sliver.data.FilmFormat
import app.sliver.data.export.stampText
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.trim.rememberPhoto
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@Composable
fun ExportScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val id = FilmNav.id
    val renderer = remember { koin.get<FilmRenderer>() }
    val film by remember(id) { koin.get<FilmRepository>().observeFilm(id ?: "") }.collectAsState(null)
    val state by remember(id) { renderer.state(id ?: "") }.collectAsState(RenderState.Running(0))
    val done = rememberUpdatedState(onDone)

    LaunchedEffect(state, film?.renderPath) {
        if (state == RenderState.Idle && film?.renderPath != null) done.value()
    }

    var leaving by remember { mutableStateOf(false) }
    BackHandler(enabled = state is RenderState.Running) { leaving = true }

    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(SliverSpace.Gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliverText(film?.name ?: "", SliverType.DisplayM)
        when (val s = state) {
            is RenderState.Running -> {
                SliverText("Rendering your film", SliverType.BodySmall, Modifier.padding(top = SliverSpace.S2), SliverColors.TextSecondary)
                SliverText("${s.percent}%", SliverType.MonoS, Modifier.padding(top = SliverSpace.S6), SliverColors.AccentCore)
                Box(Modifier.padding(vertical = SliverSpace.S3).fillMaxWidth().height(4.dp).clip(SliverShapes.Pill).background(SliverColors.Surface3)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(s.percent.coerceIn(0, 100) / 100f).background(SliverColors.Accent))
                }
                SliverText("It keeps going if you leave the app.", SliverType.Caption, color = SliverColors.TextTertiary)
                GhostButton("Cancel", {
                    id?.let(renderer::cancel)
                    onBack()
                }, Modifier.padding(top = SliverSpace.S4))
            }
            RenderState.Failed, RenderState.Idle -> if (state == RenderState.Idle && film?.renderPath != null || film == null) {
                SliverText("Finishing up", SliverType.BodySmall, Modifier.padding(top = SliverSpace.S2), SliverColors.TextSecondary)
            } else {
                var low by remember { mutableStateOf(isStorageLow(koin.get<FileStore>().freeSpaceBytes())) }
                SliverText(
                    if (low) "$STORAGE_FULL_TITLE. $STORAGE_FULL_BODY" else "Rendering stopped. Your days are untouched.",
                    SliverType.BodySmall, Modifier.padding(vertical = SliverSpace.S4), SliverColors.TextSecondary,
                )
                PrimaryButton("Try again", {
                    low = isStorageLow(koin.get<FileStore>().freeSpaceBytes())
                    if (!low) id?.let(renderer::start)
                }, Modifier.fillMaxWidth())
                GhostButton("Back", onBack)
            }
        }
    }
    SliverBottomSheet(leaving, { leaving = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            SliverText("Keep rendering in background?", SliverType.DisplayM)
            SliverText("We'll tell you when your film is ready.", SliverType.BodySmall)
            PrimaryButton("Keep rendering", onBack)
            GhostButton("Cancel render", {
                id?.let(renderer::cancel)
                onBack()
            }, destructive = true)
        }
    }
    }
}

@Composable
fun PremiereScreen(onDone: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val id = FilmNav.id
    val film by remember(id) { koin.get<FilmRepository>().observeFilm(id ?: "") }.collectAsState(null)
    val days by remember { koin.get<DayRepository>().observeAll() }.collectAsState(emptyList())
    val reduced = LocalReducedMotion.current
    val clock = remember { Animatable(if (reduced) PREMIERE_END_MS.toFloat() else 0f) }
    val finish = rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        if (!reduced) clock.animateTo(PREMIERE_END_MS.toFloat(), tween(PREMIERE_END_MS.toInt(), easing = LinearEasing))
        else kotlinx.coroutines.delay(1000)
        finish.value()
    }

    val f = film
    val ms = clock.value.toLong()
    val ratio = if (f?.format == FilmFormat.STORY) 9f / 16f else 16f / 9f
    val cover = f?.plan(days)?.days?.firstOrNull()
    Box(Modifier.fillMaxSize().background(SliverColors.BgVoid).clickable { finish.value() }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth(if (ratio > 1f) 1f else 0.7f).aspectRatio(ratio),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(maxOf(slitFraction(ms), 0.004f)).background(SliverColors.AccentCore),
            ) {
                val thumb = cover?.let { rememberPhoto(it.thumb) }
                if (thumb != null && slitFraction(ms) > 0.1f) {
                    Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(SliverColors.BgVoid.copy(alpha = 0.55f * titleAlpha(ms))))
            }
            Column(Modifier.alpha(titleAlpha(ms)), horizontalAlignment = Alignment.CenterHorizontally) {
                SliverText("A FILM BY YOU", SliverType.MonoS, color = SliverColors.TextTertiary)
                SliverText(f?.name ?: "", SliverType.DisplayM, Modifier.padding(vertical = SliverSpace.S2))
                f?.let { SliverText(it.rangeText(), SliverType.MonoS, color = SliverColors.TextSecondary) }
            }
        }
    }
}

@Composable
fun FilmPlayerScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    val koin = remember { KoinPlatform.getKoin() }
    val id = FilmNav.id
    val film by remember(id) { koin.get<FilmRepository>().observeFilm(id ?: "") }.collectAsState(null)
    val days by remember { koin.get<DayRepository>().observeAll() }.collectAsState(emptyList())
    val share = remember { koin.get<FilmShare>() }
    val scope = rememberCoroutineScope()

    var playing by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<PlayerSeek?>(null) }
    var position by remember { mutableStateOf(0L to 0L) }
    var status by remember { mutableStateOf<String?>(null) }

    val f = film
    val path = f?.renderPath
    if (f == null || path == null) {
        Box(Modifier.fillMaxSize().background(SliverColors.BgVoid).statusBarsPadding()) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
        }
        return
    }
    val plan = remember(f, days) { f.plan(days) }
    val (pos, dur) = position
    val total = dur.takeIf { it > 0 } ?: (plan.seconds * FILM_FRAME_MS + FILM_TITLE_MS)
    val ticks = remember(plan, dur) { playerTicks(plan, dur) }
    val onTitleCard = pos >= plan.seconds * FILM_FRAME_MS
    val dateLabel = if (onTitleCard || plan.days.isEmpty()) f.name else stampText(plan.days[dayIndexAtMs(pos, plan.seconds)].date)
    val ratio = if (f.format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f

    FullscreenLandscape(fullscreen)

    @Composable
    fun Controls(modifier: Modifier) {
        Column(modifier) {
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                ticks.forEach {
                    val x = it * size.width
                    drawLine(SliverColors.LineStrong, Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
            SliverSlider((pos.toFloat() / total).coerceIn(0f, 1f), { seek = PlayerSeek((it * total).toLong()); playing = true })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                GhostButton(if (playing) "Pause" else "Play", { playing = !playing })
                GhostButton(if (muted) "Unmute" else "Mute", { muted = !muted })
                GhostButton(if (fullscreen) "Exit full screen" else "Full screen", { fullscreen = !fullscreen })
            }
        }
    }

    @Composable
    fun Video(modifier: Modifier) = FilmPlayerView(
        path, playing, muted, seek,
        onPosition = { p, d -> position = p to d },
        onEnded = { playing = false },
        modifier = modifier,
    )

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(SliverColors.BgVoid)) {
            Video(Modifier.fillMaxSize().clickable { playing = !playing })
            SliverText(dateLabel, SliverType.MonoS, Modifier.align(Alignment.TopEnd).padding(SliverSpace.S4), SliverColors.AccentCore)
            Controls(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = SliverSpace.Gutter))
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(bottom = SliverSpace.S8),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            SliverText(f.name, SliverType.Headline, Modifier.padding(start = SliverSpace.S2))
        }
        Box(
            Modifier.padding(horizontal = SliverSpace.Gutter).fillMaxWidth(if (ratio > 1f) 1f else 0.6f)
                .align(Alignment.CenterHorizontally).aspectRatio(ratio).clip(SliverShapes.M).background(SliverColors.BgVoid)
                .clickable { playing = !playing },
        ) {
            Video(Modifier.fillMaxSize())
            SliverText(dateLabel, SliverType.MonoS, Modifier.align(Alignment.TopEnd).padding(SliverSpace.S3), SliverColors.AccentCore)
        }
        Controls(Modifier.padding(horizontal = SliverSpace.Gutter))
        Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            PrimaryButton("Share film…", { share.share(path, f.name) }, Modifier.fillMaxWidth())
            SecondaryButton("Save to Photos", {
                scope.launch { status = if (share.saveToPhotos(path, f.name)) "Saved to Photos" else "Could not save" }
            }, Modifier.fillMaxWidth())
            SecondaryButton("Edit film", onEdit, Modifier.fillMaxWidth())
            status?.let { SliverText(it, SliverType.Caption, color = SliverColors.TextSecondary) }
        }
    }
}
