package org.mobilenativefoundation.trails.screen.traildetail

import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.foundation.parcel.Parcelize

@Parcelize
data class TrailDetailScreen(val trailId: String) : Screen
