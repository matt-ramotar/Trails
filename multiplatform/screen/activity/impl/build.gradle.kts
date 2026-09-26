plugins {
    id("plugin.trails.feature")
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
                implementation(projects.multiplatform.feat.savetrail.api)
                implementation(projects.multiplatform.feat.savetrail.impl)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(projects.multiplatform.data.trail.impl)
                runtimeOnly(compose.desktop.currentOs)
                // Match the SLF4J API already selected by the JVM test runtime (Ktor).
                runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
            }
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.screen.activity.impl" }
