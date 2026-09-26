plugins {
    id("plugin.trails.library")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.backend)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.trail.api" }
