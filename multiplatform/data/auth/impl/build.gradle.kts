plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.auth.api)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.auth.impl"
}
