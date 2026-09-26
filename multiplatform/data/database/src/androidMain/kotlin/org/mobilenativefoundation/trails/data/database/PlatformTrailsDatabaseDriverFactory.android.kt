package org.mobilenativefoundation.trails.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
actual class PlatformTrailsDatabaseDriverFactory(
    private val context: Context
) : TrailsDatabaseDriverFactory {
    actual override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(TrailsDatabase.Schema, context, "trails.db")
    }
}
