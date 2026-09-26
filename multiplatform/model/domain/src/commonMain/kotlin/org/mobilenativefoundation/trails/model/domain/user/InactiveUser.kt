package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.Serializable

@Serializable
sealed interface InactiveUser : LoggedInUser {
    @Serializable
    data class Properties(
        override val session: UserSession,
        override val onboardingStatus: OnboardingStatus,
        override val profile: UserProfile.Node?
    ) : LoggedInUser.Properties

    @Serializable
    data object Edges : LoggedInUser.Edges

    @Serializable
    data class Node(override val id: String, override val properties: Properties) : LoggedInUser.Node

    @Serializable
    data class Composite(override val node: Node, override val edges: Edges) : InactiveUser
}