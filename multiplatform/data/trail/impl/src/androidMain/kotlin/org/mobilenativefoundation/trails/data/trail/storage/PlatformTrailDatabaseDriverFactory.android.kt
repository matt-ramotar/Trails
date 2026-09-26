package org.mobilenativefoundation.trails.data.trail.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

actual class PlatformTrailDatabaseDriverFactory(private val context: Context) : TrailDatabaseDriverFactory {
    actual override fun open(name: String): SqlDriver = AndroidSqliteDriver(TrailDataDatabase.Schema, context, name)
}
