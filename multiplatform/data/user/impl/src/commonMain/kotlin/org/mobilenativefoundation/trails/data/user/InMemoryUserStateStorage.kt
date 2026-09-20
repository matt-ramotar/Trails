package org.mobilenativefoundation.trails.data.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import org.mobilenativefoundation.trails.model.domain.user.*

/**
 * In-memory [UserStateStorage] for tests, previews, and local development.
 *
 * This implementation is thread-safe but non-persistent; state is lost on process restart.
 *
 * @param initialUser Initial user value or `null` for an empty store
 * @see UserStateStorage for the storage contract
 */
@Inject
@ContributesBinding(AppScope::class)
class InMemoryUserStateStorage(
    initialUser: User? = Mock.Tag
) : UserStateStorage {
    private val mutex = Mutex()
    private var cached: User? = initialUser

    /**
     * Reads the in-memory cached user.
     *
     * @return Current cached [User], or `null` if none is set
     * @see write for updating the cache
     */
    override suspend fun read(): User? = mutex.withLock { cached }

    /**
     * Updates the in-memory cached user.
     *
     * @param user User to store; must be non-null
     * @see read for retrieving the current value
     */
    override suspend fun write(user: User) {
        mutex.withLock { cached = user }
    }

    /**
     * Clears the in-memory cached user.
     *
     * @see read for retrieving the cleared value
     */
    override suspend fun clear() {
        mutex.withLock { cached = null }
    }
}

private object Mock {
    val Tag = ActiveUser.Composite(
        node = ActiveUser.Node(
            "tag", ActiveUser.Properties(
                UserSession("1", "token"),
                onboardingStatus = OnboardingStatus.Complete,
                profile = UserProfile.Node(
                    UserProfile.Id("tag"),
                    UserProfile.Properties(
                        email = "tag@mattramotar.dev",
                        firstName = "Tag",
                        lastName = "Ramotar",
                        birthdate = LocalDate(2018, 7, 3),
                    )
                )
            )
        ),
        edges = ActiveUser.Edges
    )
}
