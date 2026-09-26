@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class, org.mobilenativefoundation.store6.core.DelicateStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.internal.storage

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store6.core.StoreKey
import org.mobilenativefoundation.store6.core.StoreMeta
import org.mobilenativefoundation.store6.core.StoreNamespace
import org.mobilenativefoundation.store6.core.seam.Bookkeeper
import org.mobilenativefoundation.store6.core.seam.TransactionalSourceOfTruth
import org.mobilenativefoundation.store6.mutations.storage.MutationJournalStorage
import org.mobilenativefoundation.store6.mutations.storage.MutationJournalTransaction

/**
 * Waits for complete storage operations before their drivers close. Leases do not serialize
 * operations or replace SQLDelight's transaction locks. Retire the Store first to cancel its live
 * readers, then retire this owner. Late Store6 NonCancellable bookkeeping cannot reopen access.
 * This establishes storage quiescence, not termination of Store6's private coroutine supervisor.
 */
internal class StorageLifetime {
    private data class State(val retired: Boolean = false, val active: Int = 0)
    private val state = MutableStateFlow(State())

    suspend fun <T> use(block: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        while (true) {
            val previous = state.value
            if (previous.retired) throw CancellationException("This account's storage has retired")
            if (state.compareAndSet(previous, previous.copy(active = previous.active + 1))) break
        }
        try {
            return block()
        } finally {
            // Acquire/release never suspend: nested calls must remain valid inside SQLDelight's
            // synchronous withTransaction callback, and committed returns must survive cancellation.
            while (true) {
                val previous = state.value
                if (state.compareAndSet(previous, previous.copy(active = previous.active - 1))) break
            }
        }
    }

    fun <T> reader(block: () -> Flow<T>): Flow<T> = flow { use { emitAll(block()) } }

    suspend fun retire() {
        while (true) {
            val previous = state.value
            if (state.compareAndSet(previous, previous.copy(retired = true))) break
        }
        state.first { it.active == 0 }
    }
}

internal fun <K : StoreKey, V : Any> StorageLifetime.source(
    delegate: TransactionalSourceOfTruth<K, V>,
): TransactionalSourceOfTruth<K, V> = object : TransactionalSourceOfTruth<K, V> {
    override fun reader(key: K): Flow<V?> = this@source.reader { delegate.reader(key) }
    override suspend fun write(key: K, value: V) = use { delegate.write(key, value) }
    override suspend fun delete(key: K) = use { delegate.delete(key) }
    override suspend fun deleteNamespace(namespace: StoreNamespace) = use { delegate.deleteNamespace(namespace) }
    override suspend fun deleteAll() = use { delegate.deleteAll() }
    override suspend fun <R> withTransaction(block: suspend () -> R): R = use { delegate.withTransaction(block) }
}

internal fun StorageLifetime.bookkeeper(delegate: Bookkeeper): Bookkeeper = object : Bookkeeper {
    override suspend fun recordSuccess(key: StoreKey, meta: StoreMeta) = use { delegate.recordSuccess(key, meta) }
    override suspend fun recordFailure(key: StoreKey, atEpochMillis: Long) = use { delegate.recordFailure(key, atEpochMillis) }
    override suspend fun status(key: StoreKey) = use { delegate.status(key) }
    override suspend fun forget(key: StoreKey) = use { delegate.forget(key) }
    override suspend fun markStale(key: StoreKey) = use { delegate.markStale(key) }
    override suspend fun advanceStaleWatermark(namespace: StoreNamespace) = use { delegate.advanceStaleWatermark(namespace) }
    override suspend fun advanceGlobalStaleWatermark() = use { delegate.advanceGlobalStaleWatermark() }
    override suspend fun forgetNamespace(namespace: StoreNamespace) = use { delegate.forgetNamespace(namespace) }
    override suspend fun forgetAll() = use { delegate.forgetAll() }
}

internal fun StorageLifetime.journal(delegate: MutationJournalStorage): MutationJournalStorage = object : MutationJournalStorage {
    override suspend fun <R> transaction(block: (MutationJournalTransaction) -> R): R = use { delegate.transaction(block) }
}
