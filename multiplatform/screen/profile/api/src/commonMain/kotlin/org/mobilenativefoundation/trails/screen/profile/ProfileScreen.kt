package org.mobilenativefoundation.trails.screen.profile

import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.foundation.parcel.Parcelize

@Parcelize
data class ProfileScreen(
    val userId: String? = null // null = own profile, non-null = viewing another user
) : Screen