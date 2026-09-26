plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.server.api)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.trail.api" }
