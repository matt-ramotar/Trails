package org.mobilenativefoundation.trails.data.post

import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost

@OptIn(ExperimentalStoreApi::class)
interface PostStore : MutableStore<FeedKey, List<FeedPost>>
