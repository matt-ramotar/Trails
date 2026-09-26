package org.mobilenativefoundation.trails.app.navigation

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.screen.activity.ActivityScreen
import org.mobilenativefoundation.trails.screen.collection.CollectionScreen
import org.mobilenativefoundation.trails.screen.explore.ExploreScreen
import org.mobilenativefoundation.trails.screen.foryou.ForYouScreen
import org.mobilenativefoundation.trails.screen.navigate.NavigateScreen
import org.mobilenativefoundation.trails.screen.saved.SavedScreen
import org.mobilenativefoundation.trails.screen.traildetail.TrailDetailScreen

class AppNavigationCompatibilityTest {
    @Test
    fun restoresTheCheckpointCapturedBeforePackageAndClassRenames() {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/compatibility/navigation.json"))
            .use { it.readAllBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }
        // Captured with the controller at f096775268af503c509da32fc7f5c21ef127a137.
        assertEquals("965571c1b2254db00001465d363f4dbbadcd9409118dca2c866ab4bc9c7c4186", digest)
        val encoded = bytes.decodeToString()
        val storage = InMemoryAppNavigationStorage().apply { write(encoded) }
        val controller = AppNavigationController(storage)

        assertNull(controller.persistenceError)
        assertEquals(AppNavigationController.Root.SAVED, controller.selectedRoot)
        assertEquals(listOf(TrailDetailScreen("half-dome"), ExploreScreen), controller.exploreStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("trolltunga"), ForYouScreen), controller.forYouStack.map { it.screen })
        assertEquals(listOf(NavigateScreen), controller.navigateStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("half-dome"), CollectionScreen("weekend"), SavedScreen), controller.savedStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("half-dome"), ActivityScreen), controller.activityStack.map { it.screen })
        assertEquals(ExploreViewState("  Half Dome ", TrailQuery(text = "half dome")), controller.exploreView)
        assertTrue(controller.savedAllTrails)
        assertEquals("half-dome", controller.lastOpenedTrailId)
        assertEquals(ScrollPosition(2, 27), controller.scrollPosition("EXPLORE/explore"))
        assertEquals(ScrollPosition(3, 18), controller.scrollPosition("SAVED/saved:trails"))

        controller.checkpoint()
        assertEquals(encoded, storage.read())
        controller.selectExplore()
        assertEquals(TrailDetailScreen("half-dome"), controller.backStack.topRecord?.screen)
        controller.selectForYou()
        assertEquals(TrailDetailScreen("trolltunga"), controller.backStack.topRecord?.screen)
        controller.selectNavigate()
        assertEquals(NavigateScreen, controller.backStack.topRecord?.screen)
        controller.selectActivity()
        assertEquals(TrailDetailScreen("half-dome"), controller.backStack.topRecord?.screen)
        controller.selectSavedTab()
        assertEquals(encoded, storage.read())
    }
}
