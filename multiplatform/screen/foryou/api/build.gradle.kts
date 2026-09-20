plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.screen.explore.api)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.foryou.api" }
