plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.app.bootstrap.api)
                implementation(projects.multiplatform.data.user.api)
                implementation(projects.multiplatform.foundation.coroutines)
                implementation(projects.multiplatform.screen.welcome.api)
                implementation(projects.multiplatform.data.trail.api)
                implementation(projects.multiplatform.screen.prelanding.api)
                implementation(projects.multiplatform.data.user.api)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.app.bootstrap.impl"
}
