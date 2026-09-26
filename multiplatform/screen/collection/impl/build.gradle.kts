plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.collection.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.multiplatform.feature.savetrail.api)
                implementation(projects.multiplatform.ui.trail)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.collection.impl" }
