plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.welcome.api)
                implementation(projects.multiplatform.data.user.api)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(projects.multiplatform.foundation.logging)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.welcome.impl"
}
