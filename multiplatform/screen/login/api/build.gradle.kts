plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.login.api"}
