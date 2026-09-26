plugins {
    id("plugin.trails.library")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                implementation(libs.kotlinx.serialization.json)
                api(libs.sqldelight.runtime)
                implementation(libs.store6.core)
                implementation(libs.store6.sqldelight)
                implementation(libs.store6.mutations)
                implementation(libs.store6.mutations.sqldelight)
            }
        }
        androidMain { dependencies { implementation(libs.sqldelight.android) } }
        jvmMain { dependencies { implementation(libs.sqldelight.jvm) } }
        nativeMain { dependencies { implementation(libs.sqldelight.native) } }
    }
}

sqldelight {
    databases {
        create("TrailDataDatabase") { packageName.set("org.mobilenativefoundation.trails.data.trail.storage.db") }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.trail.impl" }
