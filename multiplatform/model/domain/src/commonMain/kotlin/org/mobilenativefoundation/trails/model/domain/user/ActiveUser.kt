package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.Serializable

@Serializable
sealed interface ActiveUser : LoggedInUser {
    @Serializable
    data class Properties(
        override val session: UserSession,
        override val onboardingStatus: OnboardingStatus,
        override val profile: UserProfile.Node
    ) : LoggedInUser.Properties

    @Serializable
    data object Edges : LoggedInUser.Edges

    @Serializable
    data class Node(override val id: String, override val properties: Properties) : LoggedInUser.Node

    @Serializable
    data class Composite(override val node: Node, override val edges: Edges) : ActiveUser

    companion object {
        fun fromInactiveUser(
            user: InactiveUser,
            session: UserSession = user.node.properties.session,
            onboardingStatus: OnboardingStatus = user.node.properties.onboardingStatus,
            profile: UserProfile.Node
        ): Composite = Composite(
            node = Node(
                id = user.node.id,
                properties = Properties(
                    session = session,
                    onboardingStatus = onboardingStatus,
                    profile = profile
                ),
            ),
            edges = Edges
        )
    }
}