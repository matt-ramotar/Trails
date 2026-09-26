package org.mobilenativefoundation.trails.data.user

import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.trails.model.domain.user.User

/**
 * Store5-backed mutable store for the current [User].
 *
 * This store uses [CurrentUserKey] as the only valid key and emits domain [User] values.
 *
 * @see UserStoreFactory for construction details
 * @see UserRepository for a simplified consumer API
 */
@OptIn(ExperimentalStoreApi::class)
interface UserStore : MutableStore<CurrentUserKey, User>
