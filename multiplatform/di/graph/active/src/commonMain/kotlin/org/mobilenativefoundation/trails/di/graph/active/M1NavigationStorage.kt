package org.mobilenativefoundation.trails.di.graph.active

/** Private account view checkpoint; writes either complete or throw. Never a domain-data store. */
interface M1NavigationStorage {
    fun read(): String?
    fun write(value: String)
}

fun interface M1NavigationStorageFactory {
    fun forAccount(accountId: String): M1NavigationStorage
}

class InMemoryM1NavigationStorage : M1NavigationStorage {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(value: String) { this.value = value }
}

/** Explicit fallback for platforms whose durable UI lifecycle is outside the Android M1 gate. */
class InMemoryM1NavigationStorageFactory : M1NavigationStorageFactory {
    private val partitions = mutableMapOf<String, M1NavigationStorage>()
    override fun forAccount(accountId: String): M1NavigationStorage = partitions.getOrPut(accountId) { InMemoryM1NavigationStorage() }
}
