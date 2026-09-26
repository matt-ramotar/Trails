plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
                api(libs.atom.core)
                api(projects.multiplatform.model.domain)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.profile.api"}
