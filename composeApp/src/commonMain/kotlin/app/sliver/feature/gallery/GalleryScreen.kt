package app.sliver.feature.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.sliver.core.navigation.Route
import app.sliver.core.permissions.PermissionController
import app.sliver.core.permissions.PermissionGate
import app.sliver.core.permissions.PermissionKind
import app.sliver.core.permissions.PermissionStatus
import app.sliver.core.permissions.rememberPermissionController
import app.sliver.core.theme.SliverColors
import app.sliver.core.theme.SliverSpace
import app.sliver.core.theme.SliverText
import app.sliver.core.theme.SliverType
import app.sliver.core.time.todayLocal
import app.sliver.core.ui.Chip
import app.sliver.core.ui.GhostButton
import app.sliver.core.ui.IconButton44
import app.sliver.data.DaySource
import app.sliver.feature.camera.CaptureSession
import app.sliver.feature.trim.dayLabel
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@Composable
fun GalleryScreen(onNavigate: (Route) -> Unit) {
    val controller = rememberPermissionController()
    Box(Modifier.fillMaxSize().background(SliverColors.BgBase)) {
        PermissionGate(PermissionKind.Photos, controller, onSkip = { onNavigate(Route.Camera) }) { status ->
            GalleryContent(status, controller, onNavigate)
        }
    }
}

@Composable
private fun GalleryContent(status: PermissionStatus, controller: PermissionController, onNavigate: (Route) -> Unit) {
    val provider = remember { KoinPlatform.getKoin().get<GalleryProvider>() }
    val scope = rememberCoroutineScope()
    val today = remember { todayLocal() }
    val forDate = remember { CaptureSession.forDate }
    var items by remember { mutableStateOf<List<GalleryItem>?>(null) }
    var album by remember { mutableStateOf(GalleryAlbum.Recents) }
    var filter by remember { mutableStateOf(GalleryFilter.All) }
    var picking by remember { mutableStateOf(false) }

    LaunchedEffect(status) { items = provider.load() }

    fun pick(item: GalleryItem) {
        if (picking) return
        picking = true
        scope.launch {
            val path = runCatching { provider.copyToTmp(item.uri) }.getOrNull()
            picking = false
            if (path == null) return@launch
            CaptureSession.path = path
            CaptureSession.forDate = forDate
            if (item.isVideo) {
                CaptureSession.source = DaySource.GALLERY
                onNavigate(Route.Trim)
            } else {
                CaptureSession.source = DaySource.PHOTO
                onNavigate(Route.PhotoTrim)
            }
        }
    }

    val groups = remember(items, album, filter) { visibleGroups(items.orEmpty(), album, filter) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton44({ onNavigate(Route.Camera) }, "Close") {
                SliverText("X", SliverType.MonoS, color = SliverColors.TextPrimary)
            }
            if (forDate != null) Chip("FOR " + dayLabel(forDate, today).uppercase(), selected = true, onClick = {})
        }
        if (status == PermissionStatus.Partial) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SliverText("Limited access", SliverType.BodySmall, color = SliverColors.TextSecondary)
                Row {
                    GhostButton("Select more", onClick = { controller.request(PermissionKind.Photos) })
                    GhostButton("Allow all", onClick = controller::openSettings)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S2),
            horizontalArrangement = Arrangement.spacedBy(SliverSpace.S2),
        ) {
            GalleryAlbum.entries.forEach { Chip(it.label, album == it, { album = it }) }
            Box(Modifier.weight(1f))
            GalleryFilter.entries.forEach { Chip(it.label, filter == it, { filter = it }) }
        }
        if (items != null && groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SliverText("Nothing here yet.", SliverType.BodySmall, color = SliverColors.TextSecondary)
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            groups.forEach { group ->
                item(key = "h" + group.date, span = { GridItemSpan(maxLineSpan) }) {
                    SliverText(
                        dayLabel(group.date, today),
                        SliverType.Title,
                        Modifier.padding(horizontal = SliverSpace.Gutter, vertical = SliverSpace.S3),
                        SliverColors.TextPrimary,
                    )
                }
                items(group.items, key = { it.uri }) { Cell(it, provider) { pick(it) } }
            }
        }
    }
}

@Composable
private fun Cell(item: GalleryItem, provider: GalleryProvider, onClick: () -> Unit) {
    var thumb by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.uri) { thumb = provider.thumbnail(item.uri, 256) }
    Box(Modifier.aspectRatio(1f).background(SliverColors.Surface1).clickable(onClick = onClick)) {
        thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (item.isVideo) {
            SliverText(
                durationLabel(item.durationMs), SliverType.MonoS,
                Modifier.align(Alignment.BottomEnd).padding(SliverSpace.S1), SliverColors.TextPrimary,
            )
        }
    }
}
