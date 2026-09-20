package org.mobilenativefoundation.trails.data.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InMemorySessionStore : SessionStore {
    private val mutex = Mutex()
    private var token: String? = null

    override suspend fun currentToken(): String? = mutex.withLock { token }

    override suspend fun setCurrentToken(token: String?) {
        mutex.withLock {
            this.token = token
        }
    }
}
