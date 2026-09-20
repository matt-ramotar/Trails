package org.mobilenativefoundation.trails.foundation.networking

import kotlin.concurrent.Volatile

object AuthTokenHolder {
    @Volatile
    var token: String? = null
}