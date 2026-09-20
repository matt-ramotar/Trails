plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.graph.app)
                api(libs.kotlinx.serialization.core)
                api(projects.multiplatform.foundation.designsystem)
                implementation(libs.circuit.foundation)
                implementation(libs.circuit.runtime)
            }
        }

        jvmTest {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                runtimeOnly(compose.desktop.currentOs)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.compose.activity)
                implementation(libs.androidx.core)
                implementation(libs.androidx.viewmodel)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.app"
}
