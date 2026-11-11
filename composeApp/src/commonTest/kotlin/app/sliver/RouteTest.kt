package app.sliver

import app.sliver.core.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {
    @Test
    fun startDestinationFollowsOnboardingFlag() {
        assertEquals(Route.Onboarding, Route.start(false))
        assertEquals(Route.Camera, Route.start(true))
    }

    @Test
    fun tabBarOnlyOnArchiveAndFilms() {
        assertEquals(setOf(Route.Archive, Route.Films), Route.entries.filter { it.tabBar }.toSet())
    }

    @Test
    fun pathsAreUnique() {
        assertEquals(Route.entries.size, Route.entries.map { it.path }.toSet().size)
    }
}
