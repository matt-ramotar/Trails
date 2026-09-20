package org.mobilenativefoundation.trails.data.post

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
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost

@OptIn(ExperimentalStoreApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RealPostStore(
    factory: PostStoreFactory,
) : PostStore {
    private val delegate: MutableStore<FeedKey, List<FeedPost>> = factory.create()

    override fun <Response : Any> stream(request: StoreReadRequest<FeedKey>): Flow<StoreReadResponse<List<FeedPost>>> =
        delegate.stream<Response>(request)

    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<FeedKey, List<FeedPost>, Response>>,
    ): Flow<StoreWriteResponse> = delegate.stream(requestStream)

    override suspend fun <Response : Any> write(
        request: StoreWriteRequest<FeedKey, List<FeedPost>, Response>,
    ): StoreWriteResponse = delegate.write(request)

    override suspend fun clear(key: FeedKey) {
        delegate.clear(key)
    }
}
