plugins {
    id("plugin.trails.library")
    id("plugin.trails.di")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(libs.sqldelight.runtime) }
        androidMain.dependencies { implementation(libs.sqldelight.android) }
        nativeMain.dependencies { implementation(libs.sqldelight.native) }
        jvmMain.dependencies { implementation(libs.sqldelight.jvm) }
        webMain.dependencies { implementation(libs.sqldelight.js) }
    }
}

sqldelight {
    databases {
        create("TrailsDatabase") { packageName.set("org.mobilenativefoundation.trails.data.database") }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.database" }
