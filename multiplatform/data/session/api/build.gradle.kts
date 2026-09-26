plugins {
    id("plugin.trails.library")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.datetime)
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.json)
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.session.api" }
