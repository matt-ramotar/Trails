plugins {
    id("plugin.trails.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.backend" }
