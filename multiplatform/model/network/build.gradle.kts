plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.model.domain)
                implementation(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.model.network"
}
