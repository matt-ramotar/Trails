plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.compose")
}

android {
    namespace = "org.mobilenativefoundation.trails.integration.preview"
    compileSdk = 36
    defaultConfig {
        // Retain the installed fixture identity for update compatibility.
        applicationId = "org.mobilenativefoundation.trails.c3"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "consumer-fixture"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("com.slack.circuit:circuit-foundation:0.30.0")
    implementation("app.cash.sqldelight:android-driver:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(compose.runtime)
}
