package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class RunRecord(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val stats: RunStats,
    val status: RunStatus,
    val liftAccess: List<String>,
    val features: List<String>,
    val version: Long,
)

@Serializable
data class RunStats(
    val lengthMiles: Float,
    val verticalFeet: Int,
    val averageGradePct: Int,
    val maxGradePct: Int,
)

@Serializable
data class RunStatus(
    val isOpen: Boolean,
    val conditions: String,
    val lastGroomed: String?,
)

@Serializable
data class RunSummary(
    val id: String,
    val resortId: String,
    val name: String,
    val difficulty: TrailDifficulty,
    val verticalFeet: Int,
    val distance: String,
    val liftAccess: String,
)

@Serializable
data class RunFilter(
    val difficulty: List<TrailDifficulty>? = null,
    val isOpen: Boolean? = null,
    val minVertical: Int? = null,
    val hasNightSkiing: Boolean? = null,
)

@Serializable
data class RunHistoryEntry(
    val id: String,
    val run: RunSummary,
    val resort: ResortSummary,
    val timestamp: String,
    val duration: String,
    val topSpeed: String,
)
