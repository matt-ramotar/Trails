plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.di.scope)
                implementation(projects.multiplatform.feat.filters.api)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.explore.api" }
