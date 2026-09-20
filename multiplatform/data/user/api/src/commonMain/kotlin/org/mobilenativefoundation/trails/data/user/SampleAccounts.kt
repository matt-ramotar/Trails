package org.mobilenativefoundation.trails.data.user

import kotlinx.datetime.LocalDate
import org.mobilenativefoundation.trails.model.domain.user.*

/** Explicit local preview identities. These tokens do not authenticate with a remote service. */
object SampleAccounts {
    val primary = account("trails-demo-alex", "Alex")
    val secondary = account("trails-demo-robin", "Robin")

    private fun account(id: String, name: String) = ActiveUser.Composite(
        ActiveUser.Node(id, ActiveUser.Properties(
            session = UserSession("sample-session-$id", "local-sample-$id"),
            onboardingStatus = OnboardingStatus.Complete,
            profile = UserProfile.Node(UserProfile.Id("profile-$id"), UserProfile.Properties(
                email = "$id@example.invalid", firstName = name, lastName = "",
                birthdate = LocalDate(1992, 2, 12),
            )),
        )),
        ActiveUser.Edges,
    )
}
