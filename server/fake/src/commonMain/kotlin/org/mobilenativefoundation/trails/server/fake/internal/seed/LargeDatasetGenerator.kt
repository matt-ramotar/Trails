package org.mobilenativefoundation.trails.server.fake.internal.seed

import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredPost
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredResort
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredRun
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredUser
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredWeather

internal class LargeDatasetGenerator(
) {
    private val seed = DefaultSeedData()

    val users: List<StoredUser> = seed.users
    val resorts: List<StoredResort> = seed.resorts
    val runs: List<StoredRun> = seed.runs
    val weather: List<StoredWeather> = seed.weather

    fun generatePosts(): List<StoredPost> {
        return seed.posts
    }
}
