package org.mobilenativefoundation.trails.data.session

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import org.mobilenativefoundation.store6.core.ExperimentalStoreApi
import org.mobilenativefoundation.store6.core.Freshness
import org.mobilenativefoundation.store6.core.StoreError
import org.mobilenativefoundation.store6.core.StoreException
import org.mobilenativefoundation.store6.core.StoreResult
import org.mobilenativefoundation.store6.core.seam.runtime
import org.mobilenativefoundation.store6.core.store
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.data.session.model.LoggedOutUser
import org.mobilenativefoundation.trails.data.session.model.User
import org.mobilenativefoundation.trails.foundation.coroutines.Io

/** Restores local session identity before account stores open. */
@OptIn(ExperimentalStoreApi::class)
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RealUserRepository(
    databaseQueries: TrailsDatabaseQueries,
    @param:Named("AppCoroutineScope") appScope: CoroutineScope,
    persistenceDispatcher: CoroutineDispatcher = Dispatchers.Io,
) : UserRepository {
    private val state = MutableStateFlow<User?>(null)
    private val store = store<CurrentUserKey, User> {
        fetcher { error("The session store only supports local reads") }
        persistence(UserSourceOfTruth(databaseQueries.userStateQueries, persistenceDispatcher))
    }
    private val writer = requireNotNull(store.runtime()).writeHandle

    init {
        requireNotNull(appScope.coroutineContext[Job]) { "The session store requires an app lifecycle Job" }
            .invokeOnCompletion { store.close() }
    }

    override fun stream(): Flow<User> = store
        .stream(CurrentUserKey, Freshness.LocalOnly)
        .transform { result ->
            when (result) {
                is StoreResult.Data -> emit(result.value)
                is StoreResult.Error -> when (val failure = result.error) {
                    is StoreError.Missing -> emit(LoggedOutUser)
                    else -> throw failure.asThrowable()
                }
                else -> Unit
            }
        }
        .onEach { state.value = it }

    override val current: User get() = state.value ?: LoggedOutUser

    override suspend fun persist(user: User) {
        try {
            writer.apply(CurrentUserKey, user)
        } catch (failure: StoreException) {
            throw failure.error.asThrowable()
        }
    }
}

private fun StoreError.asThrowable(): Throwable = when (this) {
    is StoreError.Persistence -> cause ?: IllegalStateException(message)
    is StoreError.Conversion -> cause ?: IllegalStateException(message)
    is StoreError.Fetch -> cause ?: IllegalStateException(message)
    is StoreError.FreshnessUnsatisfiable -> IllegalStateException(message)
    is StoreError.Conflict -> IllegalStateException(message)
    is StoreError.Missing -> IllegalStateException(message)
}
