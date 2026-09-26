package org.mobilenativefoundation.trails.data.user

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.model.domain.user.LoggedOutUser
import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Read-only access to the current [User] state.
 *
 * @see UserRepository for the combined read/write contract
 */
interface UserStateReader {
    /**
     * Returns a hot [Flow] that emits whenever the current [User] changes.
     *
     * The returned flow is safe to collect concurrently and always emits a value
     * (typically [LoggedOutUser]) as its initial state.
     *
     * @return Hot stream of user state updates
     * @sample org.mobilenativefoundation.trails.data.user.samples.observeCurrentUser
     * @see current for the latest cached snapshot
     */
    fun stream(): Flow<User>

    /**
     * The latest cached [User] value.
     *
     * @return Current user state snapshot
     * @see stream for continuous updates
     */
    val current: User
}
