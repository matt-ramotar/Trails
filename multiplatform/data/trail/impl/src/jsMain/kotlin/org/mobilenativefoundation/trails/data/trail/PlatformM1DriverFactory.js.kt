package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver

actual class PlatformM1DriverFactory : M1DriverFactory {
    actual override fun open(name: String): SqlDriver =
        error("M1 durable Store6 persistence requires a synchronous SQLDelight driver; Web storage is not implemented")
}
