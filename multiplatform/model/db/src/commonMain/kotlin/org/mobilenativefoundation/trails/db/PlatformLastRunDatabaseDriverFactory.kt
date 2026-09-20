package org.mobilenativefoundation.trails.db

import app.cash.sqldelight.db.SqlDriver

expect class PlatformTrailsDatabaseDriverFactory : TrailsDatabaseDriverFactory {
    override fun createDriver(): SqlDriver
}