package org.mobilenativefoundation.trails.data.user

import dev.zacsweers.metro.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.*
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.foundation.coroutines.Io
import org.mobilenativefoundation.trails.model.domain.user.*

/** Restores local session identity before account stores open. M1 does not simulate remote auth. */
@OptIn(ExperimentalStoreApi::class)
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RealUserRepository(
    @param:Named("AppCoroutineScope") private val appScope: CoroutineScope,
    private val userStore: UserStore,
    private val logger: Logger,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.Io,
) : UserRepository {
    private val state = MutableStateFlow<User?>(null)

    // Errors reach the bootstrap recovery surface. A failed row read is not a signed-out session.
    override fun stream(): Flow<User> = userStore
        .stream<Unit>(StoreReadRequest.cached(CurrentUserKey, refresh = false))
        .transform { response ->
            when (response) {
                is StoreReadResponse.Data -> emit(response.value)
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> error(response.message)
                else -> Unit
            }
        }.onEach { state.value = it }

    override val current: User get() = state.value ?: LoggedOutUser

    override fun updateState(user: User) {
        appScope.launch(persistenceDispatcher) {
            try { persist(user) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { logger.error(TAG, "Failed to write user state.", failure) }
        }
    }

    override suspend fun persist(user: User) = withContext(persistenceDispatcher) {
        when (val response = userStore.write(StoreWriteRequest.of<CurrentUserKey, User, Unit>(CurrentUserKey, user))) {
            is StoreWriteResponse.Error.Exception -> throw response.error
            is StoreWriteResponse.Error.Message -> error(response.message)
            else -> Unit
        }
    }

    private companion object { const val TAG = "RealUserRepository" }
}
