package app.sliver.feature.autoimport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sliver.core.icons.SliverIcon
import app.sliver.core.icons.SliverIcons
import app.sliver.core.navigation.AppNavigator
import app.sliver.core.navigation.LocalNavigator
import app.sliver.core.navigation.Route
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverShapes
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.ui.IconButton44
import app.sliver.core.ui.Notices
import app.sliver.core.ui.Segmented
import app.sliver.core.ui.SlitLoader
import app.sliver.data.db.Day
import app.sliver.data.files.FileStore
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.loadIntoSession
import app.sliver.feature.gallery.rememberMediaThumb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform

/** Moves between drafts and between the sources of one day, always through the editor screens already on the stack. */
class DraftNav(
    private val actions: DraftActions,
    private val gallery: GalleryProvider,
    private val files: FileStore,
    private val navigator: AppNavigator,
    private val scope: CoroutineScope,
) {
    var busy by mutableStateOf(false)
        private set

    /** One tap: every draft and auto-filled day counts as reviewed (undo toast restores them). */
    fun keepAll() = run { actions.keepAll() }
    fun keepOne(day: Day) = run { actions.keepDays(listOf(day)) }
    fun removeUnchecked() = run { actions.removeUnchecked() }
    fun removeOne(day: Day) = run { actions.skip(day) }

    private fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun show(day: Day, uri: String? = null, asVideo: Boolean? = null): Boolean {
        val route = actions.open(day, uri, asVideo, CaptureSession.draft?.chain ?: true)
        if (route == null) Notices.show("Couldn't read that item. Pick another.") else navigator.swapEditor(route)
        return route != null
    }

    /** First entry from the calendar or the header chip: pushes the editor. */
    fun openFirst(onNavigate: (Route) -> Unit, day: Day? = null, uri: String? = null, chain: Boolean = true) = run {
        val target = day ?: actions.first() ?: return@run
        val route = actions.open(target, uri, chain = chain)
        if (route == null) Notices.show("Couldn't read that item. Pick another.") else onNavigate(route)
    }

    fun swap(ctx: DraftContext, uri: String) = run { if (uri != ctx.selectedUri) show(ctx.day, uri) }
    fun next(ctx: DraftContext) = run { actions.next(ctx.day.date)?.let { show(it) } }
    fun previous(ctx: DraftContext) = run { actions.previous(ctx.day.date)?.let { show(it) } }

    /** After a keep or skip: the next draft, or back to where the review started. */
    suspend fun afterResolved(date: LocalDate, finished: () -> Unit) {
        val next = if (CaptureSession.draft?.chain == false) null else actions.next(date)
        if (next == null || !show(next)) finished()
    }

    fun skip(ctx: DraftContext) = run {
        actions.skip(ctx.day)
        afterResolved(ctx.day.date) { navigator.back() }
    }

    /** Video / Photo for a Motion Photo, in a draft or after the Photo Picker. */
    fun switchKind(asVideo: Boolean) = run {
        val draft = CaptureSession.draft
        if (draft != null) {
            show(draft.day, draft.selectedUri, asVideo)
        } else {
            val uri = CaptureSession.sourceUri ?: return@run
            val route = loadIntoSession(gallery, files, uri, CaptureSession.forDate, asVideo, CaptureSession.liveVideoUri)
            if (route == null) {
                Notices.show("Couldn't read that item. Pick another.")
            } else {
                navigator.swapEditor(route)
            }
        }
    }

    suspend fun keep(ctx: DraftContext, request: app.sliver.data.repo.SaveClipRequest): Result<Day> =
        runCatching { actions.keep(ctx, request) }
}

@Composable
fun rememberDraftNav(): DraftNav {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    return remember(navigator) {
        val koin = KoinPlatform.getKoin()
        DraftNav(koin.get(), koin.get(), koin.get(), navigator, scope)
    }
}

/** "Draft · 3 of 12" with Previous / Next draft. */
@Composable
fun DraftBar(ctx: DraftContext, nav: DraftNav, modifier: Modifier = Modifier) {
    val actions = remember { KoinPlatform.getKoin().get<DraftActions>() }
    val list by remember { actions.drafts }.collectAsState(emptyList())
    val index = list.indexOfFirst { it.date == ctx.day.date }.coerceAtLeast(0)
    val several = list.size > 1
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton44({ if (several) nav.previous(ctx) }, "Previous draft", Modifier.alpha(if (several) 1f else 0.35f)) {
            SliverIcon(SliverIcons.ChevronLeft, SliverColors.TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            SliverText(draftPositionLabel(index, list.size.coerceAtLeast(1)), SliverType.Meta, color = SliverColors.TextPrimary)
            if (nav.busy) SlitLoader(width = 32.dp)
        }
        IconButton44({ if (several) nav.next(ctx) }, "Next draft", Modifier.alpha(if (several) 1f else 0.35f)) {
            SliverIcon(SliverIcons.ChevronRight, SliverColors.TextPrimary)
        }
    }
}

/** All gallery items of the draft's day; tapping one swaps the source in place. */
@Composable
fun CandidateStrip(ctx: DraftContext, nav: DraftNav, modifier: Modifier = Modifier) {
    if (ctx.candidates.size < 2) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
        SliverText("Other items from this day", SliverType.Caption, color = SliverColors.TextTertiary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2)) {
            items(ctx.candidates, key = { it.uri }) { c -> CandidateTile(c, c.uri == ctx.selectedUri, Modifier.size(64.dp)) { nav.swap(ctx, c.uri) } }
        }
    }
}

@Composable
internal fun CandidateTile(c: DraftCandidate, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val thumb = rememberMediaThumb(c.uri)
    val kind = when {
        c.motion -> "Motion photo"
        c.video -> "Video, ${durationLabel(c.durationMs)}"
        else -> "Photo"
    }
    Box(
        modifier.clip(SliverShapes.S).background(SliverColors.Surface3)
            .border(if (selected) 2.dp else 1.dp, if (selected) SliverColors.AccentMark else SliverColors.LineHairline, SliverShapes.S)
            .semantics { this.selected = selected; contentDescription = kind }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (c.video && !c.motion) {
            SliverText(
                durationLabel(c.durationMs), SliverType.Meta.copy(fontSize = 10.sp, lineHeight = 12.sp),
                Modifier.align(Alignment.BottomEnd).padding(3.dp).clip(SliverShapes.S).background(SliverColors.Scrim).padding(horizontal = 4.dp),
                SliverColors.OnMedia,
            )
        }
        if (c.motion) {
            SliverText(
                "Motion", SliverType.Caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                Modifier.align(Alignment.BottomStart).padding(3.dp).clip(SliverShapes.S).background(SliverColors.Scrim).padding(horizontal = 4.dp),
                SliverColors.OnMedia,
            )
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp).clip(SliverShapes.Pill).background(SliverColors.Scrim), Alignment.Center) {
                SliverIcon(SliverIcons.Check, SliverColors.OnMedia, size = 12.dp)
            }
        }
    }
}

fun durationLabel(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** "Video · Photo" for a Motion Photo; Video is the default. */
@Composable
fun MotionChoice(video: Boolean, nav: DraftNav, modifier: Modifier = Modifier) {
    Segmented(listOf("Video", "Photo"), if (video) 0 else 1, { nav.switchKind(it == 0) }, modifier)
}
