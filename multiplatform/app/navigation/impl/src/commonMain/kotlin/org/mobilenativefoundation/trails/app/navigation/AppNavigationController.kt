package org.mobilenativefoundation.trails.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Navigator
import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.screen.activity.ActivityScreen
import org.mobilenativefoundation.trails.screen.collection.CollectionScreen
import org.mobilenativefoundation.trails.screen.explore.ExploreScreen
import org.mobilenativefoundation.trails.screen.foryou.ForYouScreen
import org.mobilenativefoundation.trails.screen.navigate.NavigateScreen
import org.mobilenativefoundation.trails.screen.saved.SavedScreen
import org.mobilenativefoundation.trails.screen.traildetail.TrailDetailScreen

/** Account-owned independent roots. A tab change never pushes another root destination. */
class AppNavigationController(private val storage: AppNavigationStorage = InMemoryAppNavigationStorage()) : AppNavigation {
    enum class Root { EXPLORE, FOR_YOU, NAVIGATE, SAVED, ACTIVITY }
    var persistenceError by mutableStateOf<String?>(null)
        private set
    private var restoreFailed = false
    private var lastWritten: String? = null
    private var view = restore()
    var selectedRoot by mutableStateOf(view.root)
        private set
    val exploreStack = restoreStack(view.exploreRoutes)
    val forYouStack = restoreStack(view.forYouRoutes)
    val navigateStack = restoreStack(view.navigateRoutes)
    val savedStack = restoreStack(view.savedRoutes)
    val activityStack = restoreStack(view.activityRoutes)
    /** Root order matches the pill: Explore, For You, Navigate, Saved, Activity. */
    val allStacks: List<SaveableBackStack> get() = listOf(exploreStack, forYouStack, navigateStack, savedStack, activityStack)
    private val navigators = Root.entries.associateWith { root -> Navigator(stack(root)) {} }
    val backStack get() = stack(selectedRoot)
    val navigator get() = navigators.getValue(selectedRoot)

    override val exploreView get() = view.explore
    override val savedAllTrails get() = view.allTrails
    override val lastOpenedTrailId: String? get() = view.lastTrail

    override fun selectExplore() { selectedRoot = Root.EXPLORE; changed() }
    override fun selectForYou() { selectedRoot = Root.FOR_YOU; changed() }
    override fun selectNavigate() { selectedRoot = Root.NAVIGATE; changed() }
    override fun selectActivity() { selectedRoot = Root.ACTIVITY; changed() }
    fun selectSavedTab() { selectedRoot = Root.SAVED; changed() }
    override fun selectSaved(collectionId: String?) {
        selectedRoot = Root.SAVED
        // Explicit success actions open the named destination; tab selection preserves its stack.
        while (savedStack.size > 1) navigator.pop()
        if (collectionId != null) navigator.goTo(CollectionScreen(collectionId))
        changed()
    }
    override fun openTrail(trailId: String) {
        navigator.goTo(TrailDetailScreen(trailId))
        if (view.lastTrail != trailId) view = view.copy(lastTrail = trailId)
        changed()
    }
    override fun back() { navigator.pop(); changed() }

    override fun checkpointExplore(value: ExploreViewState) {
        if (view.explore == value) return
        val queryChanged = view.explore.query != value.query
        view = view.copy(explore = value, scroll = if (queryChanged) view.scroll + ("EXPLORE/explore" to ScrollPosition()) else view.scroll)
        changed()
    }

    override fun checkpointSavedSegment(allTrails: Boolean) {
        if (view.allTrails == allTrails) return
        view = view.copy(allTrails = allTrails)
        changed()
    }

    /** Capture this key before an asynchronous UI callback so tab switches cannot redirect it. */
    override fun viewKey(route: String): String = "${selectedRoot.name}/$route"
    override fun scrollPosition(key: String): ScrollPosition = view.scroll[key] ?: ScrollPosition()
    override fun checkpointScroll(key: String, position: ScrollPosition) {
        if (scrollPosition(key) == position) return
        view = view.copy(scroll = (view.scroll - key + (key to position)).entries.toList().takeLast(64).associate { it.toPair() })
        changed()
    }

    /** Also called by the host after platform-driven Back changes its Circuit stack. */
    fun checkpoint() {
        if (restoreFailed) return // Preserve an unreadable prior snapshot until an explicit action.
        try {
            val encoded = view.copy(
                root = selectedRoot,
                exploreRoutes = routes(exploreStack), forYouRoutes = routes(forYouStack), navigateRoutes = routes(navigateStack),
                savedRoutes = routes(savedStack), activityRoutes = routes(activityStack),
            ).encode()
            if (encoded != lastWritten) { storage.write(encoded); lastWritten = encoded }
            persistenceError = null
        } catch (failure: Exception) {
            persistenceError = "Couldn’t save your place. Your saved trails are kept."
        }
    }

    /** Explicitly saves the current place after a read/write error; never changes domain data. */
    fun retryCheckpoint() { changed() }
    private fun changed() { restoreFailed = false; checkpoint() }

    private fun stack(root: Root): SaveableBackStack = when (root) {
        Root.EXPLORE -> exploreStack
        Root.FOR_YOU -> forYouStack
        Root.NAVIGATE -> navigateStack
        Root.SAVED -> savedStack
        Root.ACTIVITY -> activityStack
    }

    private fun restore(): AppNavigationCheckpoint = try {
        storage.read()?.let { encoded -> AppNavigationCheckpoint.decode(encoded).also { lastWritten = encoded } } ?: AppNavigationCheckpoint()
    } catch (failure: Exception) {
        restoreFailed = true
        persistenceError = "Couldn’t restore your place. Your saved trails are kept."
        AppNavigationCheckpoint()
    }

    private fun routes(stack: SaveableBackStack): List<AppRoute> = stack.map { record ->
        when (val screen = record.screen) {
            ExploreScreen -> AppRoute("explore")
            ForYouScreen -> AppRoute("foryou")
            NavigateScreen -> AppRoute("navigate")
            SavedScreen -> AppRoute("saved")
            ActivityScreen -> AppRoute("activity")
            is TrailDetailScreen -> AppRoute("trail", screen.trailId)
            is CollectionScreen -> AppRoute("collection", screen.collectionId)
            else -> error("Unsupported navigation screen")
        }
    }.reversed()

    private fun restoreStack(routes: List<AppRoute>): SaveableBackStack = SaveableBackStack(routes.first().screen()).also { stack ->
        routes.drop(1).forEach { stack.push(it.screen()) }
    }

    private fun AppRoute.screen(): Screen = when (name) {
        "explore" -> ExploreScreen
        "foryou" -> ForYouScreen
        "navigate" -> NavigateScreen
        "saved" -> SavedScreen
        "activity" -> ActivityScreen
        "trail" -> TrailDetailScreen(requireNotNull(id))
        "collection" -> CollectionScreen(requireNotNull(id))
        else -> error("Unsupported navigation route")
    }
}
