plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.auth.api"
}
