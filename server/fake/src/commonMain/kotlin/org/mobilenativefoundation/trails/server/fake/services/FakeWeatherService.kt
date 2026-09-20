package org.mobilenativefoundation.trails.server.fake.services

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables
import org.mobilenativefoundation.trails.server.fake.internal.storage.StoredWeather
import org.mobilenativefoundation.trails.server.model.ForecastRecord
import org.mobilenativefoundation.trails.server.model.SnowReportRecord
import org.mobilenativefoundation.trails.server.model.WeatherRecord
import org.mobilenativefoundation.trails.server.services.WeatherService
import kotlin.time.ExperimentalTime

internal class FakeWeatherService(
    tables: BackendTables,
    networkGate: NetworkGate,
    latencySimulator: LatencySimulator,
    errorSimulator: ErrorSimulator,
    seedLoader: SeedDataLoader,
    configProvider: BackendConfigProvider,
) : BaseFakeService(tables, networkGate, latencySimulator, errorSimulator, seedLoader, configProvider), WeatherService {

    override suspend fun getCurrentConditions(resortId: String): WeatherRecord = withSimulation {
        val weather = tables.weather().get(resortId) ?: throw ServerError.NotFound("weather", resortId)
        toWeatherRecord(weather)
    }

    override suspend fun getForecast(resortId: String, days: Int): List<ForecastRecord> = withSimulation {
        val weather = tables.weather().get(resortId) ?: throw ServerError.NotFound("weather", resortId)
        buildForecast(weather, days)
    }

    override suspend fun getSnowReport(resortId: String): SnowReportRecord = withSimulation {
        val weather = tables.weather().get(resortId) ?: throw ServerError.NotFound("weather", resortId)
        SnowReportRecord(
            resortId = resortId,
            reportedAt = epochToIso(weather.observedAt),
            newSnow24h = weather.newSnow24h,
            newSnow48h = weather.newSnow48h,
            newSnow7d = weather.newSnow7d,
            baseDepth = weather.baseDepth,
            seasonTotal = weather.seasonTotal,
            surfaceConditions = weather.conditions,
        )
    }

    override suspend fun getConditionsForResorts(resortIds: List<String>): Map<String, WeatherRecord> = withSimulation {
        resortIds.associateWith { resortId ->
            val weather = tables.weather().get(resortId) ?: return@associateWith WeatherRecord(
                id = "weather_${resortId}",
                resortId = resortId,
                observedAt = "",
                conditions = "Unknown",
                temperatureF = 0,
                feelsLikeF = 0,
                windMph = 0,
                windDirection = "",
                humidity = 0,
                visibility = "",
                uvIndex = 0,
                summary = "",
            )
            toWeatherRecord(weather)
        }
    }

    private fun toWeatherRecord(weather: StoredWeather): WeatherRecord {
        return WeatherRecord(
            id = "weather_${weather.resortId}",
            resortId = weather.resortId,
            observedAt = epochToIso(weather.observedAt),
            conditions = weather.conditions,
            temperatureF = weather.temperatureF,
            feelsLikeF = weather.feelsLikeF,
            windMph = weather.windMph,
            windDirection = weather.windDirection,
            humidity = weather.humidity,
            visibility = weather.visibility,
            uvIndex = weather.uvIndex,
            summary = summaryForConditions(weather.conditions),
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun buildForecast(weather: StoredWeather, days: Int): List<ForecastRecord> {
        val baseDate = Instant.fromEpochMilliseconds(weather.observedAt)
            .toLocalDateTime(TimeZone.UTC).date
        return (0 until days.coerceAtLeast(1)).map { offset ->
            val date = baseDate.plus(DatePeriod(days = offset)).toString()
            ForecastRecord(
                date = date,
                highF = weather.temperatureF + 2,
                lowF = weather.temperatureF - 4,
                conditions = weather.conditions,
                precipProbability = if (weather.newSnow24h > 0) 60 else 20,
                snowfallInches = weather.newSnow24h,
                windMph = weather.windMph,
            )
        }
    }

    private fun summaryForConditions(conditions: String): String {
        return when (conditions) {
            "Powder" -> "Cold powder with light wind."
            "Packed" -> "Packed base with clear visibility."
            "Groomed" -> "Smooth groomers and mild temps."
            "Wind buff" -> "Wind-buffed snow with gusty air."
            "Icy" -> "Firm surfaces with low humidity."
            else -> "Mixed conditions across the mountain."
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun epochToIso(epochMs: Long): String =
        Instant.fromEpochMilliseconds(epochMs).toString()
}
