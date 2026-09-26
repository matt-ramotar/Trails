package org.mobilenativefoundation.trails.app.runtime

import org.mobilenativefoundation.trails.app.navigation.*

import android.content.Context
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.mobilenativefoundation.trails.data.database.PlatformTrailsDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.database.createTrailsDatabaseQueries
import org.mobilenativefoundation.trails.app.navigation.AndroidAppNavigationStorageFactory
import org.mobilenativefoundation.trails.app.runtime.graph.app.AppGraph
import org.mobilenativefoundation.trails.data.trail.storage.PlatformTrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.account.RealTrailDataFactory

actual class TrailsApp(context: Context, initialOffline: Boolean = false) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val databaseQueries =
        createTrailsDatabaseQueries(PlatformTrailsDatabaseDriverFactory(context))
    init {
        // Separate debug package only: persist Offline before graph construction or service startup.
        if (initialOffline && databaseQueries.developerSettingsQueries.selectSettings().executeAsOneOrNull() == null) {
            databaseQueries.developerSettingsQueries.upsertSettings(
                1L, 0L, "SERVER_WINS", 50L, 200L, 0.0, 0L, "DISABLED", 0.0, "RANDOM",
            )
        }
    }
    private val graph: AppGraph =
        createGraphFactory<AppGraph.Factory>().createAppGraph(
            appScope, context, databaseQueries,
            RealTrailDataFactory(PlatformTrailDatabaseDriverFactory(context), appScope),
            AndroidAppNavigationStorageFactory(context),
        )
    actual val runtime = TrailsRuntime(graph, className = { it.javaClass.name })
}
