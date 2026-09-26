package org.mobilenativefoundation.trails.data.session

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store6.core.DelicateStoreApi
import org.mobilenativefoundation.store6.core.ExperimentalStoreApi
import org.mobilenativefoundation.store6.core.StoreNamespace
import org.mobilenativefoundation.store6.core.seam.SourceOfTruth
import org.mobilenativefoundation.trails.data.database.UserStateQueries
import org.mobilenativefoundation.trails.data.session.model.User

/** Adapts the existing user_state row without adding Store6 metadata tables. */
@OptIn(ExperimentalStoreApi::class, DelicateStoreApi::class)
internal class UserSourceOfTruth(
    private val queries: UserStateQueries,
    private val dispatcher: CoroutineDispatcher,
) : SourceOfTruth<CurrentUserKey, User> {
    private val access = Mutex()

    override fun reader(key: CurrentUserKey): Flow<User?> = queries
        .selectById(key.canonicalId())
        .asFlow()
        .map { query ->
            access.withLock {
                withContext(dispatcher) { query.executeAsOneOrNull()?.let(UserCodec::decode) }
            }
        }

    override suspend fun write(key: CurrentUserKey, value: User) {
        val encoded = UserCodec.encode(value)
        mutate { queries.upsert(key.canonicalId(), encoded) }
    }

    override suspend fun delete(key: CurrentUserKey) = deleteAll()

    override suspend fun deleteNamespace(namespace: StoreNamespace) {
        if (namespace.value == CurrentUserKey.namespace.value) deleteAll()
    }

    override suspend fun deleteAll() = mutate { queries.deleteAll() }

    private suspend fun mutate(block: () -> Unit) = access.withLock {
        currentCoroutineContext().ensureActive()
        // Cover both dispatcher transitions: cancellation after COMMIT must not report a failed write.
        withContext(NonCancellable) {
            withContext(dispatcher) {
                queries.transaction { block() }
            }
        }
    }
}
