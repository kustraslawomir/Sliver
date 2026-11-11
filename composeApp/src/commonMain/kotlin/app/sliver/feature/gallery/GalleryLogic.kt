package app.sliver.feature.gallery

import kotlinx.datetime.LocalDate

data class GalleryItem(
    val uri: String,
    val isVideo: Boolean,
    val date: LocalDate,
    val durationMs: Long,
    val favorite: Boolean,
)

enum class GalleryFilter(val label: String) { All("All"), Videos("Videos"), Photos("Photos") }

enum class GalleryAlbum(val label: String) { Recents("Recents"), Favorites("Favorites") }

data class DayGroup(val date: LocalDate, val items: List<GalleryItem>)

/** Newest first; items must already be sorted by taken time descending. */
fun visibleGroups(items: List<GalleryItem>, album: GalleryAlbum, filter: GalleryFilter): List<DayGroup> {
    val shown = items.filter {
        (album == GalleryAlbum.Recents || it.favorite) &&
            when (filter) {
                GalleryFilter.All -> true
                GalleryFilter.Videos -> it.isVideo
                GalleryFilter.Photos -> !it.isVideo
            }
    }
    val groups = mutableListOf<DayGroup>()
    for (item in shown) {
        val last = groups.lastOrNull()
        if (last != null && last.date == item.date) groups[groups.lastIndex] = last.copy(items = last.items + item)
        else groups += DayGroup(item.date, listOf(item))
    }
    return groups
}

fun durationLabel(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}
