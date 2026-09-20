plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.model.domain)
                implementation(projects.multiplatform.foundation.coroutines)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.data.post.api"
}
