plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.multiplatform.ui.trail)
                api(projects.multiplatform.screen.welcome.api)
                implementation(projects.multiplatform.data.session.api)
                implementation(projects.multiplatform.foundation.scope)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(projects.multiplatform.foundation.logging)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.welcome.impl"
}
