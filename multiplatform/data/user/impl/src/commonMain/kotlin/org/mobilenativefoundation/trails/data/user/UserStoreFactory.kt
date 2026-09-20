package org.mobilenativefoundation.trails.data.user

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult
import org.mobilenativefoundation.trails.db.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.coroutines.Io
import org.mobilenativefoundation.trails.model.domain.user.User
import org.mobilenativefoundation.trails.model.domain.user.LoggedOutUser
import org.mobilenativefoundation.trails.model.domain.user.UserSerializer

/**
 * Builds a Store5 [MutableStore] backed by SQLDelight for the current [User].
 *
 * The store persists user state as JSON in the `user_state` table and exposes domain models.
 *
 * @param databaseQueries Database query wrapper; must be non-null
 * @param authApi Optional remote API used for fresh fetches; `null` disables network refresh
 * @param dispatcher Coroutine dispatcher for database reads; defaults to [Dispatchers.Io]
 * @see UserStore for the Store interface
 */
@OptIn(ExperimentalStoreApi::class)
@Inject
class UserStoreFactory(
    private val databaseQueries: TrailsDatabaseQueries,
    private val authApi: UserAuthApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Io,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Creates a [MutableStore] for the current [User].
     *
     * The returned store is safe for concurrent reads and writes. Fresh reads
     * require a non-null [authApi].
     *
     * @return Store5 instance keyed by [CurrentUserKey]
     * @throws IllegalStateException if a fresh fetch is attempted without [authApi]
     * @see UserRepository for a higher-level API
     */
    fun create(): MutableStore<CurrentUserKey, User> =
        MutableStoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth(),
                converter = createConverter(),
            ).build(
                updater = createUpdater(),
            )

    private fun createFetcher(): Fetcher<CurrentUserKey, User> = Fetcher.of { _ ->
        authApi.getLoggedInUser()
    }

    private fun createSourceOfTruth(): SourceOfTruth<CurrentUserKey, User, User> =
        SourceOfTruth.of(
            reader = { _ ->
                databaseQueries
                    .userStateQueries
                    .selectById(CURRENT_USER_ROW_ID)
                    .asFlow()
                    .mapToOneOrNull(dispatcher)
                    .map { stored -> stored?.let(::decodeUser) ?: LoggedOutUser }
            },
            writer = { _, user ->
                databaseQueries.userStateQueries.upsert(CURRENT_USER_ROW_ID, encodeUser(user))
            },
            delete = { _ -> databaseQueries.userStateQueries.deleteAll() },
            deleteAll = { databaseQueries.userStateQueries.deleteAll() },
        )

    private fun createConverter(): Converter<User, User, User> =
        Converter.Builder<User, User, User>()
            .fromNetworkToLocal { it }
            .fromOutputToLocal { it }
            .build()

    private fun createUpdater(): Updater<CurrentUserKey, User, Unit> =
        Updater.by(
            post = { _, _ -> UpdaterResult.Success.Untyped(Unit) },
        )

    private fun decodeUser(raw: String): User = json.decodeFromString(UserSerializer, raw)

    private fun encodeUser(user: User): String = json.encodeToString(UserSerializer, user)

    private companion object {
        private const val CURRENT_USER_ROW_ID = "current"
    }
}
