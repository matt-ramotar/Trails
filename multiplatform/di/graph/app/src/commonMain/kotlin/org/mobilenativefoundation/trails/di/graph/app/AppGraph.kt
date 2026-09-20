package org.mobilenativefoundation.trails.di.graph.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.mobilenativefoundation.trails.foundation.coroutines.Io
import org.mobilenativefoundation.trails.data.trail.TrailDataFactory
import org.mobilenativefoundation.trails.data.trail.TrailRepository
import org.mobilenativefoundation.trails.app.AppContext
import org.mobilenativefoundation.trails.app.bootstrap.BootstrapBindings
import org.mobilenativefoundation.trails.app.bootstrap.BootstrapCoordinator
import org.mobilenativefoundation.trails.app.bootstrap.SplashStateReader
import org.mobilenativefoundation.trails.data.devsettings.DeveloperSettingsRepository
import org.mobilenativefoundation.trails.data.user.AuthTokenSynchronizer
import org.mobilenativefoundation.trails.di.graph.active.M1NavigationStorageFactory
import org.mobilenativefoundation.trails.di.graph.loggedin.LoggedInGraph
import org.mobilenativefoundation.trails.di.graph.loggedout.LoggedOutGraph
import org.mobilenativefoundation.trails.db.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.server.fake.BackendControl

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [NetworkBindings::class, BootstrapBindings::class, BackendBindings::class]
)
interface AppGraph {
    @Named("AppCoroutineScope")
    val appScope: CoroutineScope
    val bootstrapCoordinator: BootstrapCoordinator
    val authTokenSynchronizer: AuthTokenSynchronizer
    val backendConfigSynchronizer: BackendConfigSynchronizer
    val developerSettingsRepository: DeveloperSettingsRepository
    val backendControl: BackendControl?
    val trailData: TrailDataFactory
    val userRepository: org.mobilenativefoundation.trails.data.user.UserRepository

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
            @Provides navigationStorageFactory: M1NavigationStorageFactory
        ): AppGraph
    }
}
