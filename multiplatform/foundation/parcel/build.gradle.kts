plugins {
    id("plugin.trails.library")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.parcelize)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.core)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.foundation.parcel"
}
