package org.mobilenativefoundation.trails.app.runtime

import org.mobilenativefoundation.trails.feature.developertools.*

import org.mobilenativefoundation.trails.app.navigation.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import java.lang.reflect.Proxy
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mobilenativefoundation.trails.app.runtime.bootstrap.AppRoot
import org.mobilenativefoundation.trails.app.runtime.bootstrap.BootstrapCoordinator
import org.mobilenativefoundation.trails.data.developersettings.*
import org.mobilenativefoundation.trails.data.trail.*
import org.mobilenativefoundation.trails.data.trail.catalog.*
import org.mobilenativefoundation.trails.data.trail.account.*
import org.mobilenativefoundation.trails.data.trail.saved.*
import org.mobilenativefoundation.trails.data.trail.activity.*
import org.mobilenativefoundation.trails.data.trail.recommendation.*
import org.mobilenativefoundation.trails.app.runtime.graph.active.ActiveGraph
import org.mobilenativefoundation.trails.app.navigation.AppNavigationController
import org.mobilenativefoundation.trails.app.runtime.graph.app.*
import org.mobilenativefoundation.trails.feature.filters.*
import org.mobilenativefoundation.trails.feature.savetrail.SaveTrailFeature
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.screen.activity.*
import org.mobilenativefoundation.trails.screen.explore.ExploreScreen
import org.mobilenativefoundation.trails.screen.traildetail.TrailDetailScreen

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ActivityDeveloperLifetimeTest {
    @BeforeTest fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @AfterTest fun resetDispatcher() { Dispatchers.resetMain() }

    @Test
    fun actualControllerRetiresActionsOnNavigationAndGraphReplacement() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            runDesktopComposeUiTest(width = 900, height = 1800) {
                val settingsState = MutableStateFlow(DeveloperSettings())
                val settings = proxy(DeveloperSettingsRepository::class.java) { method ->
                    when (method) { "getCurrent" -> settingsState.value; "stream" -> settingsState; "readFailures" -> kotlinx.coroutines.flow.flowOf(null); else -> error(method) }
                }
                val account = proxy(TrailAccount::class.java) { error(it) }
                val calls = mutableListOf<String>()
                val registrations = mutableMapOf<String, ActivityDeveloperActions>()
                val firstNavigation = AppNavigationController().apply { selectActivity() }
                fun active(name: String, navigation: AppNavigationController): ActiveGraph {
                    val state = ActivityState(LoadState(emptyList(), false), emptyMap(), LoadState(loading = false), 0L) {
                        if (it == ActivityIntent.Retry) calls += name
                    }
                    fun presenter() = object : Presenter<ActivityState> {
                        @Composable override fun present() = state
                    }
                    val circuit = Circuit.Builder()
                        .addUi<ActivityScreen, ActivityState> { value, modifier ->
                            val actions = LocalActivityDeveloperActions.current
                            SideEffect { if (actions != null) registrations[name] = actions }
                            ActivityUi().Content(value, modifier)
                        }
                        .addPresenter<ActivityScreen, ActivityState> { _, _, _ -> presenter() }
                        .addUi<ExploreScreen, ActivityState> { _, _ -> androidx.compose.material3.Text("Explore test destination") }
                        .addPresenter<ExploreScreen, ActivityState> { _, _, _ -> presenter() }
                        .addUi<TrailDetailScreen, ActivityState> { _, _ -> androidx.compose.material3.Text("Detail test destination") }
                        .addPresenter<TrailDetailScreen, ActivityState> { _, _, _ -> presenter() }
                        .build()
                    return proxy(ActiveGraph::class.java) { method ->
                        when (method) {
                            "getAccount" -> account
                            "getCircuit" -> circuit
                            "getNavigation" -> navigation
                            "getSaves" -> Saves
                            "getFilters" -> Filters
                            else -> error(method)
                        }
                    }
                }
                val route = MutableStateFlow<AppRoot>(AppRoot.Main(active("first", firstNavigation)))
                val bootstrap = object : BootstrapCoordinator {
                    override val route = route
                    override fun start() = Unit
                }
                val trailData = proxy(TrailDataFactory::class.java) { method ->
                    if (method == "applyBackendConfig") Unit else error(method)
                }
                val sync = BackendConfigSynchronizer(scope, settings, trailData, Logger())
                val graph = proxy(AppGraph::class.java) { method ->
                    when (method) {
                        "getBootstrapCoordinator" -> bootstrap
                        "getBackendConfigSynchronizer" -> sync
                        "getDeveloperSettingsRepository" -> settings
                        "getUserRepository" -> null
                        else -> error(method)
                    }
                }
                val controller = MainViewController(graph)
                val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
                setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) { controller.Content() } }
                openDrawer()
                onNodeWithText("Session").performScrollTo().performClick()
                onNodeWithText("Try again").performScrollTo().performClick()
                runOnIdle { assertEquals(listOf("first"), calls); firstNavigation.openTrail("half-dome") }
                onNodeWithText("Try again").assertDoesNotExist()
                runOnIdle { registrations.getValue("first").retry(); assertEquals(1, calls.size); firstNavigation.back() }
                onNodeWithText("Try again").assertExists()
                runOnIdle { firstNavigation.selectExplore() }
                onNodeWithText("Try again").assertDoesNotExist()
                runOnIdle { firstNavigation.selectActivity() }
                onNodeWithText("Try again").assertExists()
                // Same account identity, new graph: the host itself must be replaced.
                runOnIdle { route.value = AppRoot.Main(active("replacement", AppNavigationController().apply { selectActivity() })) }
                runOnIdle { registrations.getValue("first").retry(); assertEquals(1, calls.size) }
                openDrawer()
                onNodeWithText("Session").performScrollTo().performClick()
                onNodeWithText("Try again").performScrollTo().performClick()
                runOnIdle { assertEquals(listOf("first", "replacement"), calls); route.value = AppRoot.Splash }
                runOnIdle { registrations.getValue("replacement").retry(); assertEquals(2, calls.size) }
            }
        } finally { scope.cancel() }
    }

    private fun ComposeUiTest.openDrawer() {
        onRoot().performTouchInput { swipe(Offset(1f, height / 2f), Offset(650f, height / 2f), durationMillis = 400) }
        onNodeWithContentDescription("Close developer tools").assertIsDisplayed()
    }

    private fun <T> proxy(type: Class<T>, read: (String) -> Any?): T = type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { instance, method, args ->
            when (method.name) {
                "equals" -> instance === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(instance)
                "toString" -> type.simpleName
                else -> read(method.name)
            }
        },
    )

    private object Saves : SaveTrailFeature {
        override fun open(trail: Trail, onDismiss: () -> Unit) = Unit
        @Composable override fun Content(onViewSaved: (String?) -> Unit) = Unit
        @Composable override fun Toast(modifier: Modifier) = Unit
    }
    private object Filters : FiltersFeature {
        override suspend fun show(query: TrailQuery, section: FilterSection, onDismiss: () -> Unit): TrailQuery? = null
        @Composable override fun Content() = Unit
    }
}
