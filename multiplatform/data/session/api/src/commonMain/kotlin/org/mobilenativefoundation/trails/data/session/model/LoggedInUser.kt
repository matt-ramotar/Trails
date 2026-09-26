package org.mobilenativefoundation.trails.data.session.model

import kotlinx.serialization.Serializable

@Serializable(with = LoggedInUserSerializer::class)
sealed interface LoggedInUser : User {
    interface Properties {
        val session: UserSession
        val onboardingStatus: OnboardingStatus
        val profile: UserProfile.Node?
    }

    interface Edges

    interface Node {
        val id: String
        val properties: Properties
    }

    val node: Node
    val edges: Edges
}