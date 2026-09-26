package org.mobilenativefoundation.trails.data.session

import kotlinx.datetime.LocalDate
import org.mobilenativefoundation.trails.data.session.model.ActiveUser
import org.mobilenativefoundation.trails.data.session.model.OnboardingStatus
import org.mobilenativefoundation.trails.data.session.model.UserProfile
import org.mobilenativefoundation.trails.data.session.model.UserSession

/** Local preview identities. These tokens do not authenticate with a remote service. */
object SampleAccounts {
    val primary = account("trails-demo-alex", "Alex")
    val secondary = account("trails-demo-robin", "Robin")

    private fun account(id: String, name: String) = ActiveUser.Composite(
        ActiveUser.Node(
            id,
            ActiveUser.Properties(
                session = UserSession("sample-session-$id", "local-sample-$id"),
                onboardingStatus = OnboardingStatus.Complete,
                profile = UserProfile.Node(
                    UserProfile.Id("profile-$id"),
                    UserProfile.Properties(
                        email = "$id@example.invalid",
                        firstName = name,
                        lastName = "",
                        birthdate = LocalDate(1992, 2, 12),
                    ),
                ),
            ),
        ),
        ActiveUser.Edges,
    )
}
