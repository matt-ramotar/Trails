plugins {
    id("plugin.trails.kotlin.android.library")
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.coil.compose)
                implementation(projects.multiplatform.foundation.networking)
                implementation(libs.coil.network.ktor3)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.google.fonts)
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

android {
    namespace = "org.mobilenativefoundation.trails.foundation.designsystem"
}
