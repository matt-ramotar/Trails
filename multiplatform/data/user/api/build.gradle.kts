plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.datetime)
                api(projects.multiplatform.model.network)
                api(projects.multiplatform.model.domain)
                implementation(projects.multiplatform.foundation.coroutines)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.user.api"
}
