plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.devsettings.api)
                implementation(projects.multiplatform.model.db)
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
    namespace = "org.mobilenativefoundation.trails.data.devsettings.impl"
}
