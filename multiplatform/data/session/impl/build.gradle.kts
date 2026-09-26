plugins {
    id("plugin.trails.library")
    id("plugin.trails.di")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.data.session.api)
            implementation(projects.multiplatform.data.database)
            implementation(projects.multiplatform.foundation.coroutines)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.store6.core)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.jvm)
            implementation(libs.turbine)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.session.impl" }
