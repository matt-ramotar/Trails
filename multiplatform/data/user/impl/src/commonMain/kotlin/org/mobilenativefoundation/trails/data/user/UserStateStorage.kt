package org.mobilenativefoundation.trails.data.user

import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Persists and restores the current [User] state.
 *
 * Implementations must be thread-safe because calls may occur from multiple coroutines.
 *
 * @see UserRepository for the high-level API
 */
interface UserStateStorage {
    /**
     * Reads the persisted user state.
     *
     * @return Stored [User], or `null` when no state is persisted
     * @see write for persisting a new value
     */
    suspend fun read(): User?

    /**
     * Persists a new user state.
     *
     * @param user User to persist; must be non-null
     * @see read for retrieving the current value
     */
    suspend fun write(user: User)

    /**
     * Clears any persisted user state.
     *
     * @see read for retrieving the cleared value
     */
    suspend fun clear()
}
