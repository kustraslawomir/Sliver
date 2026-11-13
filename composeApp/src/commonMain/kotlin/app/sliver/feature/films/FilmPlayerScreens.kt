package app.sliver.feature.films
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton

import app.sliver.core.theme.ReelogType

import androidx.compose.foundation.layout.BoxScope
import app.sliver.core.APP_NAME
import app.sliver.core.ui.OnAppHidden
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
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
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.feature.camera.rememberDeviceRotation
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
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(SliverSpace.Gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SliverText(film?.name ?: "", SliverType.DisplayM)
        when (val s = state) {
            is RenderState.Running -> {
                SliverText("Rendering your film", SliverType.BodySmall, Modifier.padding(top = SliverSpace.S2), SliverColors.TextSecondary)
                SliverText("${s.percent}%", SliverType.Meta, Modifier.padding(top = SliverSpace.S6), SliverColors.AccentCore)
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
                SliverText("A film by you", SliverType.Meta, color = SliverColors.TextTertiary)
                SliverText(f?.name ?: "", SliverType.DisplayM, Modifier.padding(vertical = SliverSpace.S2))
                f?.let { SliverText(it.plan(days).subtitle(), SliverType.Meta, color = SliverColors.TextSecondary) }
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

    var playing by remember { mutableStateOf(!FilmPlayback.paused) }
    LaunchedEffect(playing) { FilmPlayback.paused = !playing }
    OnAppHidden { playing = false }
    var muted by remember { mutableStateOf(FilmPlayback.muted) }
    LaunchedEffect(muted) { FilmPlayback.muted = muted }
    var fullscreen by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<PlayerSeek?>(null) }
    var position by remember { mutableStateOf(0L to 0L) }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val f = film
    val path = f?.renderPath
    if (f == null || path == null) {
        Box(Modifier.fillMaxSize().background(SliverColors.BgVoid).statusBarsPadding()) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
        }
        return
    }
    val plan = remember(f, days) { f.plan(days) }
    val info = remember(path) { filmInfoLine(share.sizeBytes(path)) }
    val (pos, dur) = position
    val total = dur.takeIf { it > 0 } ?: (plan.clips * FILM_FRAME_MS + FILM_TITLE_MS)
    val ticks = remember(plan, dur) { playerTicks(plan, dur) }
    val ratio = if (f.format == FilmFormat.CINEMA) 16f / 9f else 9f / 16f

    // Owner: a 9:16 film (Lena) goes full screen upright; only 16:9 films turn sideways (and follow the phone).
    val cinema = f.format == FilmFormat.CINEMA
    val landscape = rememberDeviceRotation().let { it in 60..120 || it in 240..300 }
    LaunchedEffect(landscape) { if (cinema) fullscreen = landscape }
    FullscreenLandscape(fullscreen, rotate = cinema)

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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SliverText(clock(pos), SliverType.Meta, color = SliverColors.TextTertiary)
                SliverText("-" + clock((total - pos).coerceAtLeast(0)), SliverType.Meta, color = SliverColors.TextTertiary)
            }
        }
    }

    /** Owner: sound and full screen sit on the video; a tap on the video plays / pauses (big play mark while paused). */
    @Composable
    fun BoxScope.VideoOverlay() {
        if (!playing) {
            // PlayerV17: 72 dp primary play disc.
            Box(Modifier.align(Alignment.Center).size(72.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SliverColors.Accent), Alignment.Center) {
                SliverIcon(SliverIcons.Play, SliverColors.OnAccent, size = 30.dp)
            }
        }
        IconButton44({ muted = !muted }, if (muted) "Unmute" else "Mute", Modifier.align(Alignment.BottomStart).padding(SliverSpace.S2), overCamera = true) {
            SliverIcon(if (muted) SliverIcons.VolumeX else SliverIcons.Volume2, SliverColors.OnMedia)
        }
        IconButton44({ fullscreen = !fullscreen }, if (fullscreen) "Exit full screen" else "Full screen", Modifier.align(Alignment.BottomEnd).padding(SliverSpace.S2), overCamera = true) {
            SliverIcon(SliverIcons.Maximize, SliverColors.OnMedia)
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
            VideoOverlay()
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Controls(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter))
            }
        }
        return
    }

    // PlayerV17: ambient light in the film's own colour (first clip's dominant colour) behind the whole screen.
    val ambient = plan.days.firstOrNull()?.color?.takeIf { it != 0 }?.let { Color(it) } ?: SliverColors.Surface3
    Column(
        Modifier.fillMaxSize().background(SliverColors.BgBase)
            .background(Brush.radialGradient(0f to ambient.copy(alpha = 0.38f), 1f to Color.Transparent, center = androidx.compose.ui.geometry.Offset(540f, 500f), radius = 1300f))
            .statusBarsPadding().navigationBarsPadding()
            .padding(bottom = SliverSpace.S8),
        verticalArrangement = Arrangement.spacedBy(SliverSpace.S4),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(onBack, "Close") { SliverIcon(SliverIcons.X, SliverColors.TextPrimary) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SliverText(f.name, SliverType.Headline.copy(fontSize = ReelogType.Size.body, lineHeight = ReelogType.lineFor(ReelogType.Size.body)), maxLines = 1)
                SliverText(
                    "${plan.spanText()} \u00B7 ${clipsText(plan.clips)} \u00B7 ${formatRuntime(plan.clips)}",
                    SliverType.BodySmall.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption)), color = SliverColors.TextTertiary, maxLines = 1,
                )
            }
            // Owner: share / edit / delete are icons, not buttons.
            IconButton44({ share.share(path, f.name) }, "Share film") { SliverIcon(SliverIcons.Share, SliverColors.TextPrimary, size = 20.dp) }
            IconButton44(onEdit, "Edit film") { SliverIcon(SliverIcons.Pencil, SliverColors.TextPrimary, size = 20.dp) }
            IconButton44({ confirmDelete = true }, "Delete film") { SliverIcon(SliverIcons.Trash2, SliverColors.Error, size = 20.dp) }
        }
        // The video takes the room that is left, so the buttons are always on screen (a 9:16 film pushed them off).
        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
            Box(
                Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f).background(SliverColors.BgVoid)
                    .clickable { playing = !playing },
            ) {
                Video(Modifier.fillMaxSize())
                VideoOverlay()
            }
        }
        Controls(Modifier.padding(horizontal = SliverSpace.Gutter))
        Column(Modifier.padding(horizontal = SliverSpace.Gutter), verticalArrangement = Arrangement.spacedBy(SliverSpace.S3)) {
            // Owner: one compact "Save" with a download icon (a copy gets the next free name, "Lena 2").
            ReelogButton("Save", {
                        scope.launch { status = if (share.saveToPhotos(path, f.name)) "Saved to Photos \u00B7 Movies/$APP_NAME" else "Could not save" }
            }, leading = SliverIcons.Download)
            status?.let { SliverText(it, SliverType.Caption, color = SliverColors.TextSecondary) }
            SliverText(
                info, SliverType.Meta.copy(fontSize = ReelogType.Size.caption, lineHeight = ReelogType.lineFor(ReelogType.Size.caption), textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                Modifier.fillMaxWidth(), color = SliverColors.TextTertiary,
            )
        }
    }
    app.sliver.core.ui.ConfirmSheet(
        confirmDelete, "Delete this film?", "The days stay in the project. The copy in your gallery stays too.", "Delete film",
        {
            scope.launch {
                if (runCatching { koin.get<FilmDeleter>().delete(f) }.isSuccess) onBack()
                else status = "Couldn't delete this film. Try again."
            }
        },
        { confirmDelete = false },
    )
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun PlayerRoundButton(size: androidx.compose.ui.unit.Dp, description: String, onClick: () -> Unit, filled: Boolean = false, icon: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (filled) SliverColors.TextPrimary else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.dp, SliverColors.LineHairline, androidx.compose.foundation.shape.CircleShape))
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}
