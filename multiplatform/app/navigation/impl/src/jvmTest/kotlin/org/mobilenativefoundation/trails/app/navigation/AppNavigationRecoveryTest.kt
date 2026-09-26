package org.mobilenativefoundation.trails.app.navigation

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.catalog.TrailDifficulty
import org.mobilenativefoundation.trails.data.trail.catalog.TrailFeature
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery
import org.mobilenativefoundation.trails.screen.collection.CollectionScreen
import org.mobilenativefoundation.trails.screen.explore.ExploreScreen
import org.mobilenativefoundation.trails.screen.saved.SavedScreen
import org.mobilenativefoundation.trails.screen.traildetail.TrailDetailScreen

import org.mobilenativefoundation.trails.screen.navigate.NavigateScreen
import org.mobilenativefoundation.trails.screen.activity.ActivityScreen
import org.mobilenativefoundation.trails.screen.foryou.ForYouScreen

class AppNavigationRecoveryTest {
    @Test
    fun newControllerRestoresBothRootsQuerySegmentsAndRoutePositionsFromFile() = withDirectory { directory ->
        val path = directory.resolve("account-a.json")
        val first = AppNavigationController(FileStorage(path))
        val query = TrailQuery(text = "alpine", region = "Alpine", difficulties = setOf(TrailDifficulty.MODERATE), minMeters = 3_000, maxMeters = 9_000, features = setOf(TrailFeature.LAKE)).normalized()
        first.checkpointExplore(ExploreViewState("  Alpine ", query))
        first.checkpointScroll("EXPLORE/explore", ScrollPosition(7, 42))
        first.openTrail("alpine-lake-loop")
        first.checkpointScroll(first.viewKey("trail:alpine-lake-loop"), ScrollPosition(offset = 190))
        first.selectSaved("weekend")
        first.openTrail("pine-ridge-trail")
        first.checkpointSavedSegment(true)
        first.checkpointScroll("SAVED/saved:trails", ScrollPosition(3, 18))

        val restored = AppNavigationController(FileStorage(path))
        assertNull(restored.persistenceError)
        assertEquals(AppNavigationController.Root.SAVED, restored.selectedRoot)
        assertEquals(listOf(TrailDetailScreen("alpine-lake-loop"), ExploreScreen), restored.exploreStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("pine-ridge-trail"), CollectionScreen("weekend"), SavedScreen), restored.savedStack.map { it.screen })
        assertEquals(ExploreViewState("  Alpine ", query), restored.exploreView)
        assertTrue(restored.savedAllTrails)
        assertEquals(ScrollPosition(7, 42), restored.scrollPosition("EXPLORE/explore"))
        assertEquals(ScrollPosition(offset = 190), restored.scrollPosition("EXPLORE/trail:alpine-lake-loop"))
        assertEquals(ScrollPosition(3, 18), restored.scrollPosition("SAVED/saved:trails"))
        restored.back()
        assertEquals(CollectionScreen("weekend"), restored.backStack.topRecord?.screen)
        restored.selectExplore()
        assertEquals(TrailDetailScreen("alpine-lake-loop"), restored.backStack.topRecord?.screen)
    }

    @Test
    fun platformPopCheckpointAndRootSwitchKeepTheExistingCaller() = withDirectory { directory ->
        val path = directory.resolve("account-a.json")
        val controller = AppNavigationController(FileStorage(path))
        controller.selectSaved("weekend")
        controller.openTrail("alpine-lake-loop")
        controller.navigator.pop() // Platform Circuit navigator mutates this same back stack.
        controller.checkpoint() // Simulates the host snapshot observer.
        controller.selectExplore()
        controller.selectSavedTab()
        val restored = AppNavigationController(FileStorage(path))
        assertEquals(listOf(CollectionScreen("weekend"), SavedScreen), restored.savedStack.map { it.screen })
    }

    @Test
    fun changedAppliedQueryResetsResultPositionButTypingAloneKeepsIt() = withDirectory { directory ->
        val storage = FileStorage(directory.resolve("account-a.json"))
        val controller = AppNavigationController(storage)
        controller.checkpointScroll("EXPLORE/explore", ScrollPosition(6, 22))
        controller.checkpointExplore(ExploreViewState("pine", TrailQuery()))
        assertEquals(ScrollPosition(6, 22), AppNavigationController(FileStorage(storage.path)).scrollPosition("EXPLORE/explore"))
        controller.checkpointExplore(ExploreViewState("pine", TrailQuery(text = "pine")))
        val restored = AppNavigationController(FileStorage(storage.path))
        assertEquals(ScrollPosition(), restored.scrollPosition("EXPLORE/explore"))
        assertEquals("pine", restored.exploreView.query.text)
    }

    @Test
    fun differentAccountFilesNeverRestoreEachOthersRoutesOrQuery() = withDirectory { directory ->
        val accountA = directory.resolve("account-a.json")
        val first = AppNavigationController(FileStorage(accountA))
        first.checkpointExplore(ExploreViewState("private view", TrailQuery(text = "private view")))
        first.selectSaved("favorites")
        val other = AppNavigationController(FileStorage(directory.resolve("account-b.json")))
        assertEquals(ExploreViewState(), other.exploreView)
        assertEquals(AppNavigationController.Root.EXPLORE, other.selectedRoot)
        assertEquals(listOf(SavedScreen), other.savedStack.map { it.screen })
        assertEquals(CollectionScreen("favorites"), AppNavigationController(FileStorage(accountA)).backStack.topRecord?.screen)
    }

    @Test
    fun failedWritePreservesLastFileAndRetryWritesCurrentViewWithoutThrowing() = withDirectory { directory ->
        val storage = FileStorage(directory.resolve("account-a.json"))
        val controller = AppNavigationController(storage)
        controller.openTrail("alpine-lake-loop")
        val accepted = storage.read()
        storage.failWrites = true
        controller.selectSaved("weekend")
        assertNotNull(controller.persistenceError)
        assertEquals(accepted, storage.read())
        assertEquals(AppNavigationController.Root.EXPLORE, AppNavigationController(FileStorage(storage.path)).selectedRoot)
        storage.failWrites = false
        controller.retryCheckpoint()
        assertNull(controller.persistenceError)
        assertEquals(CollectionScreen("weekend"), AppNavigationController(FileStorage(storage.path)).backStack.topRecord?.screen)
    }

    @Test
    fun unreadableCheckpointIsNotSilentlyOverwrittenByInitialHostObservation() = withDirectory { directory ->
        val storage = FileStorage(directory.resolve("account-a.json"))
        val damaged = "{\"version\":99}"
        storage.write(damaged)
        val controller = AppNavigationController(storage)
        assertNotNull(controller.persistenceError)
        controller.checkpoint()
        assertEquals(damaged, storage.read())
        controller.retryCheckpoint()
        assertNull(controller.persistenceError)
        assertEquals(AppNavigationController.Root.EXPLORE, AppNavigationController(FileStorage(storage.path)).selectedRoot)
    }

    @Test
    fun checkpointWrittenBeforeTheNewRootsRestoresDefaultsForThem() = withDirectory { directory ->
        val path = directory.resolve("legacy.json")
        Files.writeString(path, """{"version":1,"root":"SAVED","exploreRoutes":[{"name":"explore"}],"savedRoutes":[{"name":"saved"},{"name":"collection","id":"weekend"}],"text":"","query":{},"allTrails":false,"scroll":{}}""")
        val restored = AppNavigationController(FileStorage(path))
        assertNull(restored.persistenceError)
        assertEquals(AppNavigationController.Root.SAVED, restored.selectedRoot)
        assertEquals(listOf(CollectionScreen("weekend"), SavedScreen), restored.savedStack.map { it.screen })
        assertEquals(listOf(ForYouScreen), restored.forYouStack.map { it.screen })
        assertEquals(listOf(NavigateScreen), restored.navigateStack.map { it.screen })
        assertEquals(listOf(ActivityScreen), restored.activityStack.map { it.screen })
        assertNull(restored.lastOpenedTrailId)
    }

    @Test
    fun newRootsRestoreTheirStacksAndTheLastOpenedTrail() = withDirectory { directory ->
        val path = directory.resolve("roots.json")
        val first = AppNavigationController(FileStorage(path))
        first.selectActivity()
        first.openTrail("half-dome")
        first.selectForYou()
        first.openTrail("trolltunga")
        first.checkpointScroll(first.viewKey("foryou"), ScrollPosition(2, 9))
        first.selectNavigate()

        val restored = AppNavigationController(FileStorage(path))
        assertNull(restored.persistenceError)
        assertEquals(AppNavigationController.Root.NAVIGATE, restored.selectedRoot)
        assertEquals(listOf(TrailDetailScreen("half-dome"), ActivityScreen), restored.activityStack.map { it.screen })
        assertEquals(listOf(TrailDetailScreen("trolltunga"), ForYouScreen), restored.forYouStack.map { it.screen })
        assertEquals(listOf(NavigateScreen), restored.navigateStack.map { it.screen })
        assertEquals("trolltunga", restored.lastOpenedTrailId)
        assertEquals(ScrollPosition(2, 9), restored.scrollPosition("FOR_YOU/foryou"))
        restored.selectSavedTab()
        assertEquals(SavedScreen, restored.backStack.topRecord?.screen)
    }

    private class FileStorage(val path: Path) : AppNavigationStorage {
        var failWrites = false
        override fun read(): String? = if (Files.exists(path)) Files.readString(path) else null
        override fun write(value: String) {
            check(!failWrites) { "injected checkpoint write failure" }
            val temporary = path.resolveSibling("${path.fileName}.new")
            Files.writeString(temporary, value)
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun withDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("trails-navigation-test-")
        try { block(directory) } finally {
            Files.list(directory).use { files -> files.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(directory)
        }
    }
}
