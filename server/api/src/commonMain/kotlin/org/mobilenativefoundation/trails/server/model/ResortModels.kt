package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ResortRecord(
    val id: String,
    val name: String,
    val location: LocationRecord,
    val stats: ResortStats,
    val status: ResortStatus,
    val amenities: List<String>,
    val imageUrl: String?,
    val version: Long,
)

@Serializable
data class LocationRecord(
    val city: String,
    val state: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int,
)

@Serializable
data class ResortStats(
    val runs: Int,
    val lifts: Int,
    val verticalFeet: Int,
    val skiableAcres: Int,
    val snowfallAnnualInches: Int,
)

@Serializable
data class ResortStatus(
    val isOpen: Boolean,
    val liftsOpen: Int,
    val liftsTotal: Int,
    val runsOpen: Int,
    val runsTotal: Int,
    val lastUpdated: String,
)

@Serializable
data class ResortSummary(
    val id: String,
    val name: String,
    val location: String,
    val imageUrl: String?,
)

@Serializable
data class ResortFilter(
    val country: String? = null,
    val state: String? = null,
    val minVertical: Int? = null,
    val hasNightSkiing: Boolean? = null,
    val isOpen: Boolean? = null,
)
