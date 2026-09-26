package org.mobilenativefoundation.trails.android

import android.app.Application
import org.mobilenativefoundation.trails.app.runtime.TrailsApp

class App : Application() {
    val runtime by lazy { TrailsApp(this, initialOffline = BuildConfig.INITIAL_OFFLINE).runtime }
}
