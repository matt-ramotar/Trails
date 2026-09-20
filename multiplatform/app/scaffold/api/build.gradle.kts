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
    namespace = "org.mobilenativefoundation.trails.app.scaffold.api"
}
