package org.mobilenativefoundation.trails.android

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat


class MainActivity : ComponentActivity() {
    private val runtime by lazy { (application as App).runtime }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val isReady = runtime.isReady
                    return if (isReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        setContent {
            runtime.Content()
        }
    }
}
