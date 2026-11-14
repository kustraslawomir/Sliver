package app.sliver.feature.films

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sliver.core.ui.IconButton44
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import app.sliver.core.icons.SliverIcons
import app.sliver.data.pickerLabel
import app.sliver.core.kit.ButtonKind
import app.sliver.core.kit.ReelogButton

import app.sliver.core.theme.ReelogType

import kotlinx.coroutines.flow.map
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.HubIcons
import app.sliver.core.icons.SliverIcon
import app.sliver.core.ui.rememberTapGate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.Notices
import app.sliver.core.ui.SliverBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Badge
import app.sliver.core.ui.SecondaryButton
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.trim.rememberPhoto
import org.koin.mp.KoinPlatform

/** Films of the active project, shown under the calendar on the Project screen. */
@Composable
fun FilmsSection(onNavigate: (Route) -> Unit, onLongPress: (Film) -> Unit) {
    app.sliver.core.log.RecomposeProbe("films")
    val koin = remember { KoinPlatform.getKoin() }
    val films = remember { koin.get<FilmRepository>() }
    val days = remember { koin.get<DayRepository>() }
    // Owner: newest film on the left.
    val list by remember { films.observeAll().map { l -> l.sortedByDescending { it.renderedAt?.toEpochMilliseconds() ?: it.updatedAt } } }.collectAsStateWithLifecycle(emptyList())
    val all by remember { days.observeAll() }.collectAsStateWithLifecycle(emptyList())

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter), verticalAlignment = Alignment.CenterVertically) {
            // HubV18: "Films 3" + small filled Make film.
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SliverText("Films", SliverType.Title.copy(fontSize = ReelogType.Size.headline, lineHeight = ReelogType.lineFor(ReelogType.Size.headline)))
                if (list.isNotEmpty()) SliverText("${list.size}", SliverType.tnum(), Modifier.padding(bottom = 3.dp), color = SliverColors.TextTertiary)
            }
            ReelogButton("Make film", { onNavigate(Route.FilmBuilder) }, kind = ButtonKind.Secondary, small = true, fullWidth = false, leading = HubIcons.Plus)
        }
        // v1.9 §5.4: versions of the same film (same name) are one poster "4 versions"; tap opens the newest,
        // long-press lists them all.
        val groups = remember(list) { list.groupBy { it.name }.values.toList() }
        if (list.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = SliverSpace.Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(groups, key = { _, g -> g.first().id }, contentType = { _, _ -> "poster" }) { i, group ->
                    val film = group.first()
                    Poster(
                        film, film.plan(all), SliverColors.PosterTints[i % SliverColors.PosterTints.size], null,
                        versionCount = group.size,
                        onLongClick = { onLongPress(film) },
                    ) {
                        FilmNav.id = film.id
                        onNavigate(if (film.renderPath != null) Route.FilmPlayer else Route.Export)
                    }
                }
            }
        }
    }
}


@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Poster(film: Film, plan: FilmPlan, tint: Color, version: Int?, versionCount: Int = 1, onLongClick: () -> Unit, onClick: () -> Unit) {
    val renderer = remember { KoinPlatform.getKoin().get<FilmRenderer>() }
    val state by remember(film.id) { renderer.state(film.id) }.collectAsStateWithLifecycle(RenderState.Idle)
    Column(
        Modifier.width(PosterW).combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = rememberTapGate(onClick))
            .semantics { contentDescription = "${film.name}, ${filmRuntime(plan.clips)}" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A stack edge behind the poster when it holds several versions.
        if (versionCount > 1) Box(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(4.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(SliverColors.LineStrong),
        )
        Box(
            Modifier.fillMaxWidth().height(PosterH).clip(PosterShape)
                .background(Brush.verticalGradient(0f to tint, 0.75f to SliverColors.BgVoid, 1f to SliverColors.BgVoid))
        ) {
            // v1.8: each film its own cover (10 % in; versions step further in).
            val poster = plan.days.getOrNull(coverIndex(plan.days.size, version))?.let { rememberPhoto(it.thumb) }
            if (poster != null) {
                Image(poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to SliverColors.Scrim)))
            }
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                SliverText(
                    film.name, ReelogType.poster, Modifier.align(Alignment.BottomStart),
                    color = SliverColors.OnMedia, maxLines = 2,
                )
                if (film.renderPath != null && state == RenderState.Idle) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SliverColors.Scrim),
                        Alignment.Center,
                    ) { SliverIcon(SliverIcons.Play, SliverColors.OnMedia, size = 14.dp) }
                }
                when (val s = state) {
                    is RenderState.Running -> Badge("Rendering ${s.percent}%", Modifier.align(Alignment.TopStart), SliverColors.Accent)
                    RenderState.Failed -> Badge("Try again", Modifier.align(Alignment.TopStart).clickable { renderer.start(film.id) }, SliverColors.Accent)
                    RenderState.Idle -> Unit
                }
            }
        }
        // HubV18: 13 sp, two lines — range / length · version · look.
        Column {
            SliverText(plan.spanText(), ReelogType.caption, color = SliverColors.TextSecondary, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            SliverText(if (versionCount > 1) dotJoin("$versionCount versions", filmRuntime(plan.clips)) else posterCaption(plan.clips, version, film.look?.pickerLabel), ReelogType.caption.copy(fontFeatureSettings = "tnum"), color = SliverColors.TextSecondary, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Sheet for a long-pressed poster; lives at the screen root so it covers the whole window. */
@Composable
fun FilmActionsSheet(film: Film?, onDismiss: () -> Unit, onOpen: (Film) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var target by remember(film) { mutableStateOf<Film?>(null) }
    val all by remember { KoinPlatform.getKoin().get<FilmRepository>().observeAll() }.collectAsStateWithLifecycle(emptyList())
    // Every version of this film (same name), newest first, numbered oldest = v1.
    val versions = remember(all, film) {
        all.filter { it.name == film?.name }.sortedByDescending { it.renderedAt?.toEpochMilliseconds() ?: it.updatedAt }
    }
    SliverBottomSheet(film != null && target == null, onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SliverText(film?.name.orEmpty(), SliverType.Headline, Modifier.padding(bottom = SliverSpace.S2))
            if (versions.size > 1) {
                versions.forEachIndexed { i, v ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(SliverShapes.R12).clickable { onDismiss(); onOpen(v) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            SliverText("Version ${versions.size - i}", SliverType.Body)
                            SliverText(dotJoin(v.look?.pickerLabel, v.renderedAt?.let(::madeOn)), SliverType.Caption, color = SliverColors.TextTertiary)
                        }
                        IconButton44({ target = v }, "Delete version ${versions.size - i}") { SliverIcon(SliverIcons.Trash2, SliverColors.Error, size = 18.dp) }
                    }
                }
            } else {
                GhostButton("Delete film\u2026", { target = film }, Modifier.fillMaxWidth(), destructive = true)
            }
        }
    }
    // Owner 14 Nov: deleting is never a single tap.
    app.sliver.core.ui.ConfirmSheet(
        target != null, "Delete this film?", "The days stay in the project. The copy in your gallery stays too.", "Delete film",
        {
            val f = target
            if (f != null) scope.launch {
                runCatching { KoinPlatform.getKoin().get<FilmDeleter>().delete(f) }
                    .onFailure { Notices.show("Couldn't delete this film. Try again.") }
            }
        },
        { target = null; onDismiss() },
    )
}

/** "Made 13 Nov, 15:58". */
private fun madeOn(at: kotlin.time.Instant): String {
    val t = at.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "Made ${t.day} ${months[t.month.ordinal]}, ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}


private val PosterW = 112.dp
private val PosterH = 142.dp
private val PosterShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
