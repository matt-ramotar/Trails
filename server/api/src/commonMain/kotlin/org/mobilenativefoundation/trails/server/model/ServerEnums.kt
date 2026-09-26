package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
enum class TrailDifficulty {
    GREEN_CIRCLE,
    BLUE_SQUARE,
    BLACK_DIAMOND,
    DOUBLE_BLACK,
}

@Serializable
@JvmInline
value class BackgroundGradientId(val value: String)

@Serializable
@JvmInline
value class EmojiId(val value: String)
