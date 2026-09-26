package org.mobilenativefoundation.trails.android

import android.app.Application
import org.mobilenativefoundation.trails.app.TrailsApp

class App : Application() {
    val trailsApp by lazy { TrailsApp(this, initialOffline = BuildConfig.M1_INITIAL_OFFLINE) }
}
