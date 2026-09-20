package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserProfile {
    @Serializable
    data class Id(val value: String)

    @Serializable
    data class Properties(
        val email: String,
        val firstName: String,
        val lastName: String,
        val birthdate: LocalDate,
        val profileImageUrl: String? = null,
        val bio: String? = null,
        val location: String? = null,
        val verified: Boolean = false,
        val instagramUrl: String? = null,
        val youtubeUrl: String? = null,
        val websiteUrl: String? = null,
    )

    @Serializable
    data class Ref(val id: Id) : UserProfile

    @Serializable
    data class Node(val id: Id, val properties: Properties) : UserProfile

    @Serializable
    data class Edges(val usersFollowing: List<Node>, val usersFollowedBy: List<Node>) : UserProfile

    @Serializable
    data class Composite(val node: Node, val edges: Edges) : UserProfile
}