package org.mobilenativefoundation.trails.screen.home.child.post

import dev.mattramotar.atom.runtime.fsm.Event
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostEvent : Event {
    @Serializable
    data object RequestPostLike : PostEvent
}
