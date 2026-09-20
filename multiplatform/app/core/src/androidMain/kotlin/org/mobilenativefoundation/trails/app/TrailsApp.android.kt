package org.mobilenativefoundation.trails.app

import android.content.Context
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.mobilenativefoundation.trails.db.PlatformTrailsDatabaseDriverFactory
import org.mobilenativefoundation.trails.db.createTrailsDatabaseQueries
import org.mobilenativefoundation.trails.di.graph.active.AndroidM1NavigationStorageFactory
import org.mobilenativefoundation.trails.di.graph.app.AppGraph
import org.mobilenativefoundation.trails.data.trail.PlatformM1DriverFactory
import org.mobilenativefoundation.trails.data.trail.RealTrailDataFactory

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
    actual val graph: org.mobilenativefoundation.trails.di.graph.app.AppGraph =
        createGraphFactory<AppGraph.Factory>().createAppGraph(
            appScope, context, databaseQueries,
            RealTrailDataFactory(PlatformM1DriverFactory(context), appScope),
            AndroidM1NavigationStorageFactory(context),
        )
}
