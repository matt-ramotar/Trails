package org.mobilenativefoundation.trails.app.navigation

/** Private account view checkpoint; writes either complete or throw. Never a domain-data store. */
interface AppNavigationStorage {
    fun read(): String?
    fun write(value: String)
}

fun interface AppNavigationStorageFactory {
    fun forAccount(accountId: String): AppNavigationStorage
}

class InMemoryAppNavigationStorage : AppNavigationStorage {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(value: String) { this.value = value }
}

/** In-memory checkpoints for hosts without persistent navigation storage. */
class InMemoryAppNavigationStorageFactory : AppNavigationStorageFactory {
    private val partitions = mutableMapOf<String, AppNavigationStorage>()
    override fun forAccount(accountId: String): AppNavigationStorage = partitions.getOrPut(accountId) { InMemoryAppNavigationStorage() }
}
