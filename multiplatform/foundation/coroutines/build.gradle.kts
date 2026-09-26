plugins {
    id("plugin.trails.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.foundation.coroutines"
}