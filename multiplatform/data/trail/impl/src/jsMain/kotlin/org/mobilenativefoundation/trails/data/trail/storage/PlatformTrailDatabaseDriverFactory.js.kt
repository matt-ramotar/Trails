package org.mobilenativefoundation.trails.data.trail.storage

import app.cash.sqldelight.db.SqlDriver

actual class PlatformTrailDatabaseDriverFactory : TrailDatabaseDriverFactory {
    actual override fun open(name: String): SqlDriver =
        error("Durable trail persistence requires a synchronous SQLDelight driver; Web storage is not implemented")
}
