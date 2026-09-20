plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.metro)
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(libs.ktor.core)
                api(libs.ktor.negotiation)
                api(libs.ktor.serialization.json)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.logging)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.okhttp)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        webMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.foundation.networking"
}