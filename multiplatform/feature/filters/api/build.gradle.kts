plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.feature.filters.api" }
