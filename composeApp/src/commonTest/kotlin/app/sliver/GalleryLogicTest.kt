package app.sliver

import app.sliver.feature.gallery.GalleryAlbum
import app.sliver.feature.gallery.GalleryFilter
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.durationLabel
import app.sliver.feature.gallery.visibleGroups
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryLogicTest {
    private val d1 = LocalDate(2026, 5, 2)
    private val d2 = LocalDate(2026, 5, 1)
    private val items = listOf(
        GalleryItem("a", true, d1, 3000, false),
        GalleryItem("b", false, d1, 0, true),
        GalleryItem("c", true, d2, 9000, true),
    )

    @Test
    fun groupsByDayNewestFirst() {
        val g = visibleGroups(items, GalleryAlbum.Recents, GalleryFilter.All)
        assertEquals(listOf(d1, d2), g.map { it.date })
        assertEquals(listOf("a", "b"), g[0].items.map { it.uri })
    }

    @Test
    fun favoritesAndFilters() {
        assertEquals(listOf("b", "c"), visibleGroups(items, GalleryAlbum.Favorites, GalleryFilter.All).flatMap { it.items }.map { it.uri })
        assertEquals(listOf("a", "c"), visibleGroups(items, GalleryAlbum.Recents, GalleryFilter.Videos).flatMap { it.items }.map { it.uri })
        assertEquals(listOf("b"), visibleGroups(items, GalleryAlbum.Recents, GalleryFilter.Photos).flatMap { it.items }.map { it.uri })
    }

    @Test
    fun duration() = assertEquals("1:05", durationLabel(65_000))
}
