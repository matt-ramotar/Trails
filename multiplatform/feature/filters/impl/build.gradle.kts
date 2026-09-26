plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.feature.filters.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
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

android { namespace = "org.mobilenativefoundation.trails.feature.filters.impl" }
