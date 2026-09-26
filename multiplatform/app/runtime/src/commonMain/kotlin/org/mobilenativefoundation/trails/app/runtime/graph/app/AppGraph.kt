package org.mobilenativefoundation.trails.app.runtime.graph.app

import org.mobilenativefoundation.trails.app.navigation.*

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.mobilenativefoundation.trails.foundation.coroutines.Io
import org.mobilenativefoundation.trails.data.trail.account.TrailDataFactory
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.app.runtime.AppContext
import org.mobilenativefoundation.trails.app.runtime.bootstrap.BootstrapBindings
import org.mobilenativefoundation.trails.app.runtime.bootstrap.BootstrapCoordinator
import org.mobilenativefoundation.trails.app.runtime.bootstrap.SplashStateReader
import org.mobilenativefoundation.trails.data.developersettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.app.navigation.AppNavigationStorageFactory
import org.mobilenativefoundation.trails.app.runtime.graph.loggedin.LoggedInGraph
import org.mobilenativefoundation.trails.app.runtime.graph.loggedout.LoggedOutGraph
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.logging.Logger

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [BootstrapBindings::class]
)
internal interface AppGraph {
    @Named("AppCoroutineScope")
    val appScope: CoroutineScope
    val bootstrapCoordinator: BootstrapCoordinator
    val backendConfigSynchronizer: BackendConfigSynchronizer
    val developerSettingsRepository: DeveloperSettingsRepository
    val trailData: TrailDataFactory
    val userRepository: org.mobilenativefoundation.trails.data.session.UserRepository

    @Provides
    fun provideTrailRepository(factory: TrailDataFactory): TrailRepository = factory.trails
    val loggedOut: LoggedOutGraph.Factory
    val loggedIn: LoggedInGraph.Factory
    val splashStateReader: SplashStateReader

    @Provides
    fun providePersistenceDispatcher(): CoroutineDispatcher = Dispatchers.Io

    @Provides
    fun provideLogger(): Logger = Logger()

    @DependencyGraph.Factory
    fun interface Factory {
        fun createAppGraph(
            @Named("AppCoroutineScope") @Provides appScope: CoroutineScope,
            @Provides context: AppContext,
            @Provides databaseQueries: TrailsDatabaseQueries,
            @Provides trailDataFactory: TrailDataFactory,
            @Provides navigationStorageFactory: AppNavigationStorageFactory
        ): AppGraph
    }
}
