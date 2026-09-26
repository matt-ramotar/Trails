package org.mobilenativefoundation.trails.model.domain.post


import kotlinx.serialization.Serializable
import org.mobilenativefoundation.trails.model.domain.user.UserProfile


@Serializable
sealed interface Post {
    @Serializable
    data class Id(val value: String)

    @Serializable
    data class Properties(
        val content: String,
        val authorId: String,
    )

    @Serializable
    data class Ref(val id: Id) : Post

    @Serializable
    data class Node(val id: Id, val properties: Properties) : Post

    @Serializable
    data class Edges(val author: UserProfile.Node) : Post

    @Serializable
    data class Composite(val node: Node, val edges: Edges) : Post
}