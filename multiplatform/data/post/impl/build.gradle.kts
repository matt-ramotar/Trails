plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.post.api)
                implementation(projects.multiplatform.data.auth.api)
                implementation(projects.multiplatform.data.devsettings.api)
                implementation(projects.server.api)
                implementation(projects.multiplatform.model.db)
                implementation(projects.multiplatform.model.network)
                implementation(projects.multiplatform.foundation.coroutines)
                api(projects.multiplatform.foundation.logging)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.store5)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.sqldelight.jvm)
                implementation(projects.multiplatform.data.devsettings.impl)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.post.impl"
}
