package org.mobilenativefoundation.trails.data.post

import kotlinx.serialization.Serializable

@Serializable
data class FeedKey(
    val value: String
) {
    companion object {
        val Home: FeedKey = FeedKey("home")
    }
}
