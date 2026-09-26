package org.mobilenativefoundation.trails.data.trail.storage

import app.cash.sqldelight.db.SqlDriver

interface TrailDatabaseDriverFactory { fun open(name: String): SqlDriver }

expect class PlatformTrailDatabaseDriverFactory : TrailDatabaseDriverFactory {
    override fun open(name: String): SqlDriver
}
