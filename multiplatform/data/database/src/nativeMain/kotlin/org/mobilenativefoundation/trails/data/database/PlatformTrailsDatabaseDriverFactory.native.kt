package org.mobilenativefoundation.trails.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.data.database.TrailsDatabase
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseDriverFactory

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
actual class PlatformTrailsDatabaseDriverFactory : TrailsDatabaseDriverFactory {
    actual override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(TrailsDatabase.Schema, "trails.db")
    }
}
