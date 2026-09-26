plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.signup.api)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.signup.impl"
}
