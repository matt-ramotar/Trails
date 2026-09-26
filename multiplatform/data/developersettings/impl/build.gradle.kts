plugins {
    id("plugin.trails.library")
    id("plugin.trails.di")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.developersettings.api)
                implementation(projects.multiplatform.data.database)
                implementation(projects.multiplatform.foundation.coroutines)
                api(projects.multiplatform.foundation.logging)
                implementation(libs.sqldelight.coroutines)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.sqldelight.jvm)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.developersettings.impl"
}
