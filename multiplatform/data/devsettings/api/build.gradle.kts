plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.server.api)
                implementation(projects.multiplatform.foundation.coroutines)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.devsettings.api"
}
