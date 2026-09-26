package org.mobilenativefoundation.trails.server.fake.internal.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

internal class InMemoryBackendStore {
    private val tables = mutableMapOf<KClass<*>, BackendTable<*, *>>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <K : Any, V : Any> getTable(
        type: KClass<V>,
        keyExtractor: (V) -> K,
    ): BackendTable<K, V> = mutex.withLock {
        tables.getOrPut(type) {
            BackendTable(keyExtractor)
        } as BackendTable<K, V>
    }

    suspend fun clear() = mutex.withLock {
        tables.values.forEach { it.clear() }
    }

    suspend fun snapshot(): Map<String, List<Any>> = mutex.withLock {
        tables.map { (type, table) ->
            type.simpleName.orEmpty() to table.getAll()
        }.toMap()
    }
}
