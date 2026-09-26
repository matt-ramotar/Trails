plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.activity.api)
                implementation(projects.multiplatform.foundation.designsystem)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(projects.multiplatform.feature.savetrail.api)
                implementation(projects.multiplatform.ui.trail)
                implementation(projects.multiplatform.feature.developertools.api)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(projects.multiplatform.data.trail.impl)
                runtimeOnly(compose.desktop.currentOs)
                // Capture JDBC diagnostics from the JVM test runtime.
                runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.activity.impl" }
