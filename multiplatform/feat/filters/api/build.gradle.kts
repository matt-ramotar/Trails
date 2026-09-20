plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.di.scope)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.feat.filters.api" }
