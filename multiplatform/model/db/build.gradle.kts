plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.kotlinx.datetime)
                implementation(projects.multiplatform.di.scope)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqldelight.android)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.sqldelight.native)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.sqldelight.jvm)
            }
        }

        webMain {
            dependencies {
                implementation(libs.sqldelight.js)
            }
        }
    }
}

sqldelight {
    databases {
        create("TrailsDatabase").apply {
            packageName.set("org.mobilenativefoundation.trails.db")
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.model.db"
}
