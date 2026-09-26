package org.mobilenativefoundation.trails.screen.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mobilenativefoundation.trails.data.session.SampleAccounts
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.foundation.logging.Logger
import org.mobilenativefoundation.trails.foundation.scope.LoggedOutScope

@ContributesBinding(LoggedOutScope::class)
@Inject
class WelcomePresenter(
    private val userRepository: UserRepository,
    private val logger: Logger,
) : Presenter<WelcomeState> {
    @Composable
    override fun present(): WelcomeState {
        val coroutineScope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        return WelcomeState(isLoading, error) { intent ->
            when (intent) {
                WelcomeIntent.ExploreSampleTrails -> if (!isLoading) {
                    isLoading = true
                    error = null
                    coroutineScope.launch {
                        try {
                            // Bootstrap observes the persisted identity and opens its account stores.
                            userRepository.persist(SampleAccounts.primary)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            logger.error(TAG, "Could not save the local sample account.", failure)
                            error = "We couldn’t open your local sample. Please try again."
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "WelcomePresenter"
    }
}
