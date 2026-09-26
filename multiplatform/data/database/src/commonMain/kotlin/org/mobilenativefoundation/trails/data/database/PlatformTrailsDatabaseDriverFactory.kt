package org.mobilenativefoundation.trails.data.database

import app.cash.sqldelight.db.SqlDriver

expect class PlatformTrailsDatabaseDriverFactory : TrailsDatabaseDriverFactory {
    override fun createDriver(): SqlDriver
}