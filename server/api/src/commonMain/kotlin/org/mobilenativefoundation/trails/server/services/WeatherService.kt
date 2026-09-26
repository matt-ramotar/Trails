package org.mobilenativefoundation.trails.server.services

import org.mobilenativefoundation.trails.server.model.ForecastRecord
import org.mobilenativefoundation.trails.server.model.SnowReportRecord
import org.mobilenativefoundation.trails.server.model.WeatherRecord

interface WeatherService {
    suspend fun getCurrentConditions(resortId: String): WeatherRecord
    suspend fun getForecast(resortId: String, days: Int = 5): List<ForecastRecord>
    suspend fun getSnowReport(resortId: String): SnowReportRecord
    suspend fun getConditionsForResorts(resortIds: List<String>): Map<String, WeatherRecord>
}
