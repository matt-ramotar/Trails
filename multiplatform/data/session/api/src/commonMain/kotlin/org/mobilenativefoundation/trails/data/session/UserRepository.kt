package org.mobilenativefoundation.trails.data.session

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.trails.data.session.model.User

/** The persisted local session used to select the app's account scope. */
interface UserRepository {
    /** Emits the stored session, or a logged-out user when no row exists. Read failures propagate. */
    fun stream(): Flow<User>

    /** Last observed session. Logged out until [stream] has emitted its first value. */
    val current: User

    /** Returns after the session is committed to disk. Storage failures propagate. */
    suspend fun persist(user: User)
}
