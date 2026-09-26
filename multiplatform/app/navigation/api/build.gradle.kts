plugins {
    id("plugin.trails.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.data.trail.api)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.app.navigation.api" }
