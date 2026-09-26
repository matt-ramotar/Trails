plugins {
    id("plugin.trails.library")
    id("plugin.trails.di")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.backend)
                implementation(projects.multiplatform.foundation.coroutines)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.developersettings.api"
}
