plugins {
    id("plugin.trails.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.data.trail.api)
            api(projects.multiplatform.foundation.designsystem)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            runtimeOnly(compose.desktop.currentOs)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.ui.trail" }
