package org.mobilenativefoundation.trails.data.user

/**
 * Facade for observing and mutating the current authenticated [User].
 *
 * Implementations are expected to be thread-safe and to provide a hot user stream.
 *
 * @see UserStateReader for the read contract
 * @see UserStateWriter for the write contract
 */
interface UserRepository : UserStateReader, UserStateWriter
