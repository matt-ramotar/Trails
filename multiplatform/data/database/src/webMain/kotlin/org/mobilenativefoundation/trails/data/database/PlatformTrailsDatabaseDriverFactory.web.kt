package org.mobilenativefoundation.trails.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.data.database.TrailsDatabase
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseDriverFactory
import org.w3c.dom.Worker

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
actual class PlatformTrailsDatabaseDriverFactory :
    TrailsDatabaseDriverFactory {
    actual override fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            Worker(
                js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
            )
        ).also {
            TrailsDatabase.Schema.create(it)
        }
    }
}
