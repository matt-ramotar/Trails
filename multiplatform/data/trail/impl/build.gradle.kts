plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.data.trail.api)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sqldelight.runtime)
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
        create("M1Database") { packageName.set("org.mobilenativefoundation.trails.data.trail.db") }
    }
}

android { namespace = "org.mobilenativefoundation.trails.data.trail.impl" }
