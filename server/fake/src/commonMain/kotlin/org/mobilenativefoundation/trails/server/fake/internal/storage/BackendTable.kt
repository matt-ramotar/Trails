package org.mobilenativefoundation.trails.server.fake.internal.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class BackendTable<K : Any, V : Any>(
    private val keyExtractor: (V) -> K,
) {
    private val data = mutableMapOf<K, V>()
    private val mutex = Mutex()

    suspend fun get(key: K): V? = mutex.withLock { data[key] }

    suspend fun getAll(): List<V> = mutex.withLock { data.values.toList() }

    suspend fun put(value: V): V = mutex.withLock {
        val key = keyExtractor(value)
        data[key] = value
        value
    }

    suspend fun putAll(values: List<V>) = mutex.withLock {
        values.forEach { value ->
            data[keyExtractor(value)] = value
        }
    }

    suspend fun remove(key: K): V? = mutex.withLock { data.remove(key) }

    suspend fun clear() = mutex.withLock { data.clear() }

    suspend fun count(): Int = mutex.withLock { data.size }

    suspend fun query(predicate: (V) -> Boolean): List<V> = mutex.withLock {
        data.values.filter(predicate)
    }

    suspend fun update(key: K, transform: (V) -> V): V? = mutex.withLock {
        data[key]?.let { existing ->
            val updated = transform(existing)
            data[key] = updated
            updated
        }
    }

    suspend fun updateOrCreate(
        key: K,
        create: () -> V,
        transform: (V) -> V,
    ): V = mutex.withLock {
        val existing = data[key]
        val updated = if (existing != null) transform(existing) else create()
        data[key] = updated
        updated
    }

    suspend fun exists(key: K): Boolean = mutex.withLock { data.containsKey(key) }
}
