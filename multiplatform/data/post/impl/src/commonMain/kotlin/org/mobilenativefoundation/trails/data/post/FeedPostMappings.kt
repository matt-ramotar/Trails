package org.mobilenativefoundation.trails.data.post

import org.mobilenativefoundation.trails.model.domain.feed.FeedPost
import org.mobilenativefoundation.trails.model.network.feed.FeedPostAuthorRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostEngagementRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostResortRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostRunRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostStyleRecord
import org.mobilenativefoundation.trails.model.network.feed.FeedPostWeatherRecord

internal fun FeedPostRecord.toDomain(): FeedPost =
    FeedPost(
        id = id,
        authorId = author.id,
        username = author.username,
        displayName = author.displayName,
        avatar = author.avatar,
        verified = author.verified,
        runId = run.id,
        runName = run.name,
        resort = resort.name,
        resortId = resort.id,
        runResortId = run.resortId,
        location = resort.location,
        weatherId = weather.id,
        weatherResortId = weather.resortId,
        weatherObservedAt = weather.observedAt,
        weatherSource = weather.source,
        weatherTemperatureF = weather.temperatureF,
        weatherFeelsLikeF = weather.feelsLikeF,
        weatherWindMph = weather.windMph,
        weatherSnowfall24hIn = weather.snowfall24hIn,
        weatherVisibility = weather.visibility,
        weatherSummary = weather.summary,
        distance = run.distance,
        vertical = run.vertical,
        duration = run.duration,
        topSpeed = run.topSpeed,
        difficulty = run.difficulty,
        views = engagement.views,
        likes = engagement.likes,
        comments = engagement.comments,
        shares = engagement.shares,
        conditions = weather.conditions,
        temperature = weather.temperature,
        backgroundGradient = style.backgroundGradient,
        timestamp = timestamp,
        liftAccess = run.liftAccess,
        isLiked = engagement.isLiked,
        isBookmarked = engagement.isBookmarked,
        isFollowing = author.isFollowing,
        emoji = style.emoji,
        version = version,
    )

internal fun FeedPost.toRecord(): FeedPostRecord =
    FeedPostRecord(
        id = id,
        author = FeedPostAuthorRecord(
            id = authorId(authorId, username),
            username = username,
            displayName = displayName,
            avatar = avatar,
            verified = verified,
            isFollowing = isFollowing,
        ),
        run = FeedPostRunRecord(
            id = runId(runId, runName),
            resortId = runResortId(runResortId, resortId),
            name = runName,
            distance = distance,
            vertical = vertical,
            duration = duration,
            topSpeed = topSpeed,
            difficulty = difficulty,
            liftAccess = liftAccess,
        ),
        resort = FeedPostResortRecord(
            id = resortId(resortId, resort),
            name = resort,
            location = location,
        ),
        weather = FeedPostWeatherRecord(
            id = weatherId(weatherId, conditions, temperature),
            resortId = weatherResortId(weatherResortId, resortId),
            observedAt = weatherObservedAt,
            source = weatherSource,
            conditions = conditions,
            temperature = temperatureLabel(temperature, weatherTemperatureF),
            temperatureF = temperatureF(weatherTemperatureF, temperature),
            feelsLikeF = weatherFeelsLikeF,
            windMph = weatherWindMph,
            snowfall24hIn = weatherSnowfall24hIn,
            visibility = weatherVisibility,
            summary = weatherSummary,
        ),
        engagement = FeedPostEngagementRecord(
            views = views,
            likes = likes,
            comments = comments,
            shares = shares,
            isLiked = isLiked,
            isBookmarked = isBookmarked,
        ),
        style = FeedPostStyleRecord(
            backgroundGradient = backgroundGradient,
            emoji = emoji,
        ),
        timestamp = timestamp,
        version = version,
    )

private fun authorId(id: String, username: String): String =
    id.ifBlank { "author_${slug(username)}" }

private fun runId(id: String, name: String): String =
    id.ifBlank { "run_${slug(name)}" }

private fun runResortId(id: String, resortId: String): String =
    id.ifBlank { resortId }

private fun resortId(id: String, name: String): String =
    id.ifBlank { "resort_${slug(name)}" }

private fun weatherId(id: String, conditions: String, temperature: String): String =
    id.ifBlank {
        "weather_${slug(conditions)}_${slug(temperature)}"
    }

private fun weatherResortId(id: String, resortId: String): String =
    id.ifBlank { resortId }

private fun temperatureF(value: Int?, temperature: String): Int? =
    value ?: temperature.trim().removeSuffix("F").removeSuffix("f").toIntOrNull()

private fun temperatureLabel(temperature: String, temperatureF: Int?): String =
    if (temperature.isNotBlank()) temperature else temperatureF?.let { "${it}F" }.orEmpty()

private fun slug(value: String): String =
    value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
