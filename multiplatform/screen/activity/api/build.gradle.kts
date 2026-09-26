plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.app.navigation.api)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.activity.api" }
