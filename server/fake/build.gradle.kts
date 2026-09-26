plugins {
    id("plugin.trails.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.server.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.server.fake"
}
