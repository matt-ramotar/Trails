package org.mobilenativefoundation.trails.data.post

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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
import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord

@OptIn(ExperimentalStoreApi::class)
@Inject
class PostStoreFactory(
    private val databaseQueries: TrailsDatabaseQueries,
    private val postApi: PostApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Io,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun create(): MutableStore<FeedKey, List<FeedPost>> =
        MutableStoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth(),
                converter = createConverter(),
            ).build(
                updater = createUpdater(),
            )

    private fun createFetcher(): Fetcher<FeedKey, List<FeedPostRecord>> = Fetcher.of { key ->
        postApi.fetchFeed(key)
    }

    private fun createSourceOfTruth(): SourceOfTruth<FeedKey, List<FeedPostEntity>, List<FeedPost>> =
        SourceOfTruth.of(
            reader = { key ->
                databaseQueries
                    .postsQueries
                    .selectFeedPosts(key.value)
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { rows ->
                        rows.map { row -> decodePost(row.json) }.ifEmpty { null }
                    }
            },
            writer = { key, entities ->
                    databaseQueries.postsQueries.transaction {
                    databaseQueries.postsQueries.clearFeed(key.value)
                    entities.forEachIndexed { index, entity ->
                        databaseQueries.postsQueries.upsertPost(
                            entity.id,
                            entity.authorId,
                            entity.runId,
                            entity.runResortId,
                            entity.resortId,
                            entity.weatherId,
                            entity.weatherResortId,
                            entity.json,
                        )
                        databaseQueries.postsQueries.insertFeedPost(key.value, entity.id, index.toLong())
                    }
                }
            },
            delete = { key -> databaseQueries.postsQueries.clearFeed(key.value) },
            deleteAll = {
                databaseQueries.postsQueries.deleteAllFeedPosts()
                databaseQueries.postsQueries.deleteAllPosts()
            },
        )

    private fun createConverter(): Converter<List<FeedPostRecord>, List<FeedPostEntity>, List<FeedPost>> =
        Converter.Builder<List<FeedPostRecord>, List<FeedPostEntity>, List<FeedPost>>()
            .fromNetworkToLocal { records ->
                records.map { record ->
                    FeedPostEntity(
                        id = record.id,
                        authorId = record.author.id,
                        runId = record.run.id,
                        runResortId = record.run.resortId,
                        resortId = record.resort.id,
                        weatherId = record.weather.id,
                        weatherResortId = record.weather.resortId,
                        json = encodePost(record.toDomain()),
                    )
                }
            }
            .fromOutputToLocal { posts ->
                posts.map { post ->
                    val record = post.toRecord()
                    val normalizedPost =
                        post.copy(
                            authorId = record.author.id,
                            runId = record.run.id,
                            resortId = record.resort.id,
                            runResortId = record.run.resortId,
                            weatherId = record.weather.id,
                            weatherResortId = record.weather.resortId,
                        )
                    FeedPostEntity(
                        id = record.id,
                        authorId = record.author.id,
                        runId = record.run.id,
                        runResortId = record.run.resortId,
                        resortId = record.resort.id,
                        weatherId = record.weather.id,
                        weatherResortId = record.weather.resortId,
                        json = encodePost(normalizedPost),
                    )
                }
            }
            .build()

    private fun createUpdater(): Updater<FeedKey, List<FeedPost>, Unit> =
        Updater.by(
            post = { key, posts ->
                try {
                    postApi.updateFeed(key, posts.map { it.toRecord() })
                    UpdaterResult.Success.Untyped(Unit)
                } catch (throwable: Throwable) {
                    UpdaterResult.Error.Exception(throwable)
                }
            },
        )

    private fun decodePost(raw: String): FeedPost = json.decodeFromString(raw)

    private fun encodePost(post: FeedPost): String = json.encodeToString(post)
}
