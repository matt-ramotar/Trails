package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver

interface M1DriverFactory { fun open(name: String): SqlDriver }

expect class PlatformM1DriverFactory : M1DriverFactory {
    override fun open(name: String): SqlDriver
}
