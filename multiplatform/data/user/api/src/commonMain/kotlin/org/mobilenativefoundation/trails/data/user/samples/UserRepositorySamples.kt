package org.mobilenativefoundation.trails.data.user.samples

import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.trails.data.user.UserRepository
import org.mobilenativefoundation.trails.model.domain.user.LoggedOutUser
import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Observes the current user by suspending until the first emission.
 *
 * @param repository Repository to query; must be non-null
 * @return The first emitted [User] value
 * @see UserRepository.stream for the underlying stream
 */
suspend fun observeCurrentUser(repository: UserRepository): User {
    return repository.stream().first()
}

/**
 * Sets the user state to [LoggedOutUser].
 *
 * @param repository Repository to update; must be non-null
 * @see UserRepository.updateState for the mutation API
 */
suspend fun signOut(repository: UserRepository) {
    repository.updateState(LoggedOutUser)
}
