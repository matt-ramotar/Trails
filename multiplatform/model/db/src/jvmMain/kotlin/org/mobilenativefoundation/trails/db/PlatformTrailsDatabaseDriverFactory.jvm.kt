package org.mobilenativefoundation.trails.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.mobilenativefoundation.trails.db.TrailsDatabase
import org.mobilenativefoundation.trails.db.TrailsDatabaseDriverFactory

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
actual class PlatformTrailsDatabaseDriverFactory : TrailsDatabaseDriverFactory {
    actual override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrailsDatabase.Schema.create(driver)
        return driver
    }
}
