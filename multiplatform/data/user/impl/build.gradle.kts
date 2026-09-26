plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.user.api)
                implementation(projects.multiplatform.data.auth.api)
                implementation(projects.server.api)
                implementation(projects.multiplatform.model.db)
                implementation(projects.multiplatform.foundation.coroutines)
                api(projects.multiplatform.foundation.networking)
                api(projects.multiplatform.foundation.logging)
                api(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.store5)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.user.impl"
}
