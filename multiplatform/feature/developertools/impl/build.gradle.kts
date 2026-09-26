plugins {
    id("plugin.trails.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.feature.developertools.api)
            implementation(projects.multiplatform.data.session.api)
            implementation(projects.multiplatform.foundation.designsystem)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(libs.circuit.foundation)
            implementation(projects.multiplatform.screen.activity.impl)
            implementation(compose.desktop.uiTestJUnit4)
            runtimeOnly(compose.desktop.currentOs)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.feature.developertools.impl" }
