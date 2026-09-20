plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.collection.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.multiplatform.feat.savetrail.api)
                implementation(projects.multiplatform.feat.savetrail.impl)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.collection.impl" }
