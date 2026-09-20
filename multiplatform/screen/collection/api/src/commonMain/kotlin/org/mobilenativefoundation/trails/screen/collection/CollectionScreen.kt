package org.mobilenativefoundation.trails.screen.collection

import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.foundation.parcel.Parcelize

@Parcelize
data class CollectionScreen(val collectionId: String) : Screen
