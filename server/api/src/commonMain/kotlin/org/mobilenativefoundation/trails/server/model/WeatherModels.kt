package org.mobilenativefoundation.trails.server.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherRecord(
    val id: String,
    val resortId: String,
    val observedAt: String,
    val conditions: String,
    val temperatureF: Int,
    val feelsLikeF: Int,
    val windMph: Int,
    val windDirection: String,
    val humidity: Int,
    val visibility: String,
    val uvIndex: Int,
    val summary: String,
)

@Serializable
data class ForecastRecord(
    val date: String,
    val highF: Int,
    val lowF: Int,
    val conditions: String,
    val precipProbability: Int,
    val snowfallInches: Int,
    val windMph: Int,
)

@Serializable
data class SnowReportRecord(
    val resortId: String,
    val reportedAt: String,
    val newSnow24h: Int,
    val newSnow48h: Int,
    val newSnow7d: Int,
    val baseDepth: Int,
    val seasonTotal: Int,
    val surfaceConditions: String,
)
