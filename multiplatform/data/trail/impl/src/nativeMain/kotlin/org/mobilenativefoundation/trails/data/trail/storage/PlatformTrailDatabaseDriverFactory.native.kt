package org.mobilenativefoundation.trails.data.trail.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

actual class PlatformTrailDatabaseDriverFactory : TrailDatabaseDriverFactory {
    actual override fun open(name: String): SqlDriver = NativeSqliteDriver(TrailDataDatabase.Schema, name)
}
