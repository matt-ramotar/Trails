package org.mobilenativefoundation.trails.data.trail.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

actual class PlatformTrailDatabaseDriverFactory(
    // Keep the existing directory so upgrades reopen installed databases.
    private val directory: File = File(System.getProperty("user.home"), ".trails/m1"),
) : TrailDatabaseDriverFactory {
    actual override fun open(name: String): SqlDriver {
        require(name.matches(Regex("[a-zA-Z0-9_.-]+")))
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, name)
        val create = !file.exists()
        return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { driver ->
            if (create) TrailDataDatabase.Schema.create(driver).value
        }
    }
}
