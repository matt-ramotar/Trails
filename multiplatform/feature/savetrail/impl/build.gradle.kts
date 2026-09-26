plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.feature.savetrail.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(projects.multiplatform.ui.trail)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.atom.core)
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

android { namespace = "org.mobilenativefoundation.trails.feature.savetrail.impl" }
