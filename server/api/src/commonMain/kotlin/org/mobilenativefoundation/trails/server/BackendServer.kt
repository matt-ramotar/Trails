package org.mobilenativefoundation.trails.server

import org.mobilenativefoundation.trails.server.services.FeedService
import org.mobilenativefoundation.trails.server.services.PostService
import org.mobilenativefoundation.trails.server.services.ResortService
import org.mobilenativefoundation.trails.server.services.RunService
import org.mobilenativefoundation.trails.server.services.UserService
import org.mobilenativefoundation.trails.server.services.WeatherService

interface BackendServer {
    val userService: UserService
    val feedService: FeedService
    val postService: PostService
    val resortService: ResortService
    val runService: RunService
    val weatherService: WeatherService
}
