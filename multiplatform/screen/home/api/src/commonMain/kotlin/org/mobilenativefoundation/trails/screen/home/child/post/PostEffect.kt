package org.mobilenativefoundation.trails.screen.home.child.post

import dev.mattramotar.atom.runtime.fsm.SideEffect
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostEffect : SideEffect {
    data object UpdatePost : PostEffect
}