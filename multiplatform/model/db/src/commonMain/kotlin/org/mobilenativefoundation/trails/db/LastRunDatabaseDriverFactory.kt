package org.mobilenativefoundation.trails.db

import app.cash.sqldelight.db.SqlDriver

interface TrailsDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}