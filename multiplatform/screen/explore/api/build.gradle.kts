plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.app.navigation.api)
                api(projects.multiplatform.feature.filters.api)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.explore.api" }
