plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.login.api)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.login.impl"
}
