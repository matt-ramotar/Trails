package org.mobilenativefoundation.trails.data.user

import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Write-only access for mutating the current [User] state.
 *
 * @see UserRepository for the combined read/write contract
 */
interface UserStateWriter {
    /**
     * Updates the stored [User] state.
     *
     * Implementations may ignore updates that are equal to the current state.
     *
     * @param user New user value to persist; must be non-null
     * @sample org.mobilenativefoundation.trails.data.user.samples.signOut
     * @see UserStateReader.stream for observing updates
     */
    fun updateState(user: User)

    /** Returns only after the session row is persisted; failures propagate to the caller. */
    suspend fun persist(user: User)
}
