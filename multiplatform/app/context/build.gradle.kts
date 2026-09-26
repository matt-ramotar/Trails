plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.app.context"
}
