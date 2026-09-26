package org.mobilenativefoundation.trails.db

import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
class TrailsDatabaseQueries(
    driver: SqlDriver,
) {
    val userStateQueries: UserStateQueries = UserStateQueries(driver)
    val postsQueries: PostsQueries = PostsQueries(driver)
    val developerSettingsQueries: DeveloperSettingsQueries = DeveloperSettingsQueries(driver)
}

fun createTrailsDatabaseQueries(
    driverFactory: TrailsDatabaseDriverFactory,
): TrailsDatabaseQueries = TrailsDatabaseQueries(driverFactory.createDriver())
