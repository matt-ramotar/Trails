package org.mobilenativefoundation.trails.app.runtime

import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.trails.app.runtime.bootstrap.AppRoot
import org.mobilenativefoundation.trails.app.runtime.bootstrap.BootstrapCoordinator
import org.mobilenativefoundation.trails.app.runtime.bootstrap.SplashState
import org.mobilenativefoundation.trails.app.runtime.bootstrap.SplashStateReader
import org.mobilenativefoundation.trails.app.runtime.graph.app.AppGraph
import org.mobilenativefoundation.trails.app.runtime.graph.app.BackendConfigSynchronizer
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettings
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.developersettings.toBackendConfig
import org.mobilenativefoundation.trails.data.session.SampleAccounts
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.data.trail.account.TrailAccount
import org.mobilenativefoundation.trails.data.trail.account.TrailDataFactory
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.foundation.logging.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class TrailsRuntimeTest {
    @Test
    fun runtimeStartsSettingsSynchronizationBeforeBootstrapAndDiagnosticsOnlyReadState() = runTest {
        val events = mutableListOf<String>()
        val settings = MutableStateFlow(DeveloperSettings(offlineMode = true))
        val repository = proxy(DeveloperSettingsRepository::class.java) { method ->
            when (method) {
                "getCurrent" -> settings.value
                "stream" -> settings
                "readFailures" -> flowOf(null)
                else -> error("Unexpected settings access: $method")
            }
        }
        val trailData = object : TrailDataFactory {
            override val trails: TrailRepository get() = error("Catalog is not used during bootstrap")
            override val backendConfig = MutableStateFlow<BackendConfig?>(null)
            override suspend fun restoreBackendConfig() = requireNotNull(backendConfig.value)
            override suspend fun applyBackendConfig(config: BackendConfig) {
                events += "settings-applied"
                backendConfig.value = config
            }
            override suspend fun open(accountId: String): TrailAccount = error("Accounts are not used by this bootstrap")
            override suspend fun close() = Unit
        }
        val users = proxy(UserRepository::class.java) { method ->
            if (method == "getCurrent") SampleAccounts.primary else error(method)
        }
        val bootstrap = object : BootstrapCoordinator {
            override val route = MutableStateFlow<AppRoot>(AppRoot.Splash)
            override fun start() { events += "bootstrap-started" }
        }
        val splash = object : SplashStateReader {
            override val state = MutableStateFlow(SplashState.READY)
        }
        val testScope = backgroundScope
        val synchronizer by lazy {
            events += "synchronizer-created"
            BackendConfigSynchronizer(testScope, repository, trailData, Logger())
        }
        val graph = proxy(AppGraph::class.java) { method ->
            when (method) {
                "getBootstrapCoordinator" -> bootstrap
                "getBackendConfigSynchronizer" -> synchronizer
                "getSplashStateReader" -> splash
                "getUserRepository" -> users
                "getTrailData" -> trailData
                else -> error("Unexpected graph access: $method")
            }
        }

        val runtime = TrailsRuntime(graph, className = { it.javaClass.name })
        assertEquals(listOf("synchronizer-created", "bootstrap-started"), events)
        runCurrent()
        assertEquals(settings.value.toBackendConfig(), trailData.backendConfig.value)
        assertTrue(runtime.isReady)
        assertSame(users, runtime.userRepository)
        assertSame(trailData, runtime.trailData)

        val beforeSnapshot = events.toList()
        val snapshot = runtime.diagnosticSnapshot()
        assertEquals(AppRoot.Splash.javaClass.name, snapshot["bootstrapRouteClass"])
        assertEquals(SampleAccounts.primary.javaClass.name, snapshot["userClass"])
        assertEquals(synchronizer.status.value.toString(), snapshot["backendConfigSync"])
        assertEquals(trailData.backendConfig.value.toString(), snapshot["backendConfig"])
        assertEquals(beforeSnapshot, events, "Diagnostics must not start or retry runtime services")
    }

    private fun <T> proxy(type: Class<T>, read: (String) -> Any?): T = type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ -> read(method.name) },
    )
}
