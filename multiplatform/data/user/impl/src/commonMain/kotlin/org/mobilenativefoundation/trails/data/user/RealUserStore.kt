package org.mobilenativefoundation.trails.data.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse
import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Default [UserStore] implementation backed by Store5.
 *
 * @param factory Factory used to create the underlying store instance
 * @see UserStoreFactory for construction details
 */
@OptIn(ExperimentalStoreApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RealUserStore(
    factory: UserStoreFactory,
) : UserStore {
    private val delegate: MutableStore<CurrentUserKey, User> = factory.create()

    /**
     * Streams user state changes for the given request.
     *
     * @param request Store read request; must be non-null
     * @return Flow of [StoreReadResponse] values
     * @see UserStoreFactory for store configuration
     */
    override fun <Response : Any> stream(request: StoreReadRequest<CurrentUserKey>): Flow<StoreReadResponse<User>> =
        delegate.stream<Response>(request)

    /**
     * Streams write requests and their corresponding write responses.
     *
     * @param requestStream Stream of write requests; must be non-null
     * @return Flow of write responses in request order
     * @see write for single-request writes
     */
    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<CurrentUserKey, User, Response>>,
    ): Flow<StoreWriteResponse> = delegate.stream(requestStream)

    /**
     * Writes a single user state update to the store.
     *
     * @param request Write request; must be non-null
     * @return Result of the write operation
     * @see stream for streaming writes
     */
    override suspend fun <Response : Any> write(
        request: StoreWriteRequest<CurrentUserKey, User, Response>,
    ): StoreWriteResponse = delegate.write(request)

    /**
     * Clears cached data for the given key and its persisted entry.
     *
     * @param key Store key to clear; must be [CurrentUserKey]
     * @see UserStoreFactory for persistence configuration
     */
    override suspend fun clear(key: CurrentUserKey) {
        delegate.clear(key)
    }
}
