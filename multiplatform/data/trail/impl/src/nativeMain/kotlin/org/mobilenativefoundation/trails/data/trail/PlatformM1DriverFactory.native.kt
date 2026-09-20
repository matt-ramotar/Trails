package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.mobilenativefoundation.trails.data.trail.db.M1Database

actual class PlatformM1DriverFactory : M1DriverFactory {
    actual override fun open(name: String): SqlDriver = NativeSqliteDriver(M1Database.Schema, name)
}
