package org.mobilenativefoundation.trails.data.trail

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.mobilenativefoundation.trails.data.trail.db.M1Database

actual class PlatformM1DriverFactory(private val context: Context) : M1DriverFactory {
    actual override fun open(name: String): SqlDriver = AndroidSqliteDriver(M1Database.Schema, context, name)
}
