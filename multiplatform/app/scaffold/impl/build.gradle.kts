plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
                api(projects.multiplatform.app.scaffold.api)
                api(projects.multiplatform.screen.home.api)
                api(projects.multiplatform.screen.profile.api)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.compose.activity)
                implementation(libs.androidx.core)
                implementation(libs.androidx.viewmodel)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.app.scaffold.impl"
}
