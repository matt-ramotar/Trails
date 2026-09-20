package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import org.mobilenativefoundation.trails.data.trail.db.M1Database

actual class PlatformM1DriverFactory(
    private val directory: File = File(System.getProperty("user.home"), ".trails/m1"),
) : M1DriverFactory {
    actual override fun open(name: String): SqlDriver {
        require(name.matches(Regex("[a-zA-Z0-9_.-]+")))
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, name)
        val create = !file.exists()
        return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { driver ->
            if (create) M1Database.Schema.create(driver).value
        }
    }
}
