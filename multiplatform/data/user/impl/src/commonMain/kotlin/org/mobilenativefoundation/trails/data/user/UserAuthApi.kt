package org.mobilenativefoundation.trails.data.user

import kotlinx.datetime.LocalDate
import org.mobilenativefoundation.trails.model.domain.user.ActiveUser
import org.mobilenativefoundation.trails.model.domain.user.InactiveUser
import org.mobilenativefoundation.trails.model.domain.user.LoggedInUser

/**
 * Remote authentication API for user lifecycle operations.
 *
 * Implementations are expected to be thread-safe and perform network I/O.
 *
 * @see UserRepository for local state management
 */
interface UserAuthApi {
    /**
     * Registers a new user account.
     *
     * @param email Email address; must be non-empty and RFC 5322 compliant
     * @param password Plain-text password; must be non-empty
     * @return Newly created inactive user profile
     * @throws Exception if the request fails or the server rejects the credentials
     * @see login for signing in an existing user
     */
    suspend fun signup(email: String, password: String): InactiveUser.Composite

    /**
     * Authenticates a user with credentials.
     *
     * @param email Email address; must be non-empty and RFC 5322 compliant
     * @param password Plain-text password; must be non-empty
     * @return Logged-in user profile
     * @throws Exception if authentication fails or the request cannot be completed
     * @see logout for ending the session
     */
    suspend fun login(email: String, password: String): LoggedInUser

    /**
     * Completes onboarding for the current session.
     *
     * @param firstName Given name; must be non-empty
     * @param lastName Family name; must be non-empty
     * @param birthdate Date of birth; must be in the past
     * @return Fully active user profile
     * @throws Exception if onboarding fails or the request cannot be completed
     * @see signup for creating a new account
     */
    suspend fun completeOnboarding(
        firstName: String,
        lastName: String,
        birthdate: LocalDate
    ): ActiveUser.Composite

    /**
     * Ends the current authentication session.
     *
     * @throws Exception if the request cannot be completed
     * @see login for creating a session
     */
    suspend fun logout()

    /**
     * Fetches the current logged-in user profile from the server.
     *
     * @return Server-authoritative user profile
     * @throws Exception if the request fails or the session is invalid
     * @see UserRepository.stream for observing local state
     */
    suspend fun getLoggedInUser(): LoggedInUser
}
