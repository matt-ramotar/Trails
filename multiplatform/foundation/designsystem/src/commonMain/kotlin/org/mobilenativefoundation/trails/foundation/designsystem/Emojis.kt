package org.mobilenativefoundation.trails.foundation.designsystem

import androidx.compose.runtime.Composable
import trails.multiplatform.foundation.designsystem.generated.resources.*
import org.jetbrains.compose.resources.painterResource

object Emojis {
    val Skier
        @Composable get() = painterResource(Res.drawable.skier)

    val Snowboarder
        @Composable get() = painterResource(Res.drawable.snowboarder)

    val SnowCappedMountain
        @Composable get() = painterResource(Res.drawable.snow_capped_mountain)

    val AerialTramway
        @Composable get() = painterResource(Res.drawable.aerial_tramway)
}