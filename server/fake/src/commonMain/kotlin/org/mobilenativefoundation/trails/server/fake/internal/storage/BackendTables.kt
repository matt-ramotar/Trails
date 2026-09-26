package org.mobilenativefoundation.trails.server.fake.internal.storage

internal class BackendTables(private val store: InMemoryBackendStore) {
    suspend fun users() = store.getTable(
        type = StoredUser::class,
        keyExtractor = { it.id },
    )

    suspend fun sessions() = store.getTable(
        type = StoredSession::class,
        keyExtractor = { it.token },
    )

    suspend fun follows() = store.getTable(
        type = StoredFollow::class,
        keyExtractor = { "${it.followerId}:${it.followeeId}" },
    )

    suspend fun posts() = store.getTable(
        type = StoredPost::class,
        keyExtractor = { it.id },
    )

    suspend fun postEngagements() = store.getTable(
        type = StoredPostEngagement::class,
        keyExtractor = { "${it.userId}:${it.postId}" },
    )

    suspend fun comments() = store.getTable(
        type = StoredComment::class,
        keyExtractor = { it.id },
    )

    suspend fun resorts() = store.getTable(
        type = StoredResort::class,
        keyExtractor = { it.id },
    )

    suspend fun runs() = store.getTable(
        type = StoredRun::class,
        keyExtractor = { it.id },
    )

    suspend fun weather() = store.getTable(
        type = StoredWeather::class,
        keyExtractor = { it.resortId },
    )

    suspend fun userFavorites() = store.getTable(
        type = StoredUserFavorite::class,
        keyExtractor = { "${it.userId}:${it.resortId}" },
    )
}
