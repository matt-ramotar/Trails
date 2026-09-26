plugins {
    id("plugin.trails.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.data.developersettings.api)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.feature.developertools.api" }
