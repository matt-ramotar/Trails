package org.mobilenativefoundation.trails.data.database

import app.cash.sqldelight.db.SqlDriver

interface TrailsDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}