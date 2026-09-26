plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.traildetail.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.multiplatform.feature.savetrail.api)
                implementation(projects.multiplatform.ui.trail)
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

android { namespace = "org.mobilenativefoundation.trails.screen.traildetail.impl" }
