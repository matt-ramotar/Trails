plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.explore.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.multiplatform.feat.savetrail.api)
                implementation(projects.multiplatform.feat.savetrail.impl)
                implementation(projects.multiplatform.feat.filters.api)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.explore.impl" }
