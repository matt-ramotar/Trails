plugins {
    id("plugin.trails.android.application")
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
    alias(libs.plugins.parcelize)
}

val initialOfflinePreview = providers.gradleProperty("offlinePreview").map(String::toBoolean).getOrElse(false)
android {
    namespace = "org.mobilenativefoundation.trails.android"
    defaultConfig {
        testInstrumentationRunner = "org.mobilenativefoundation.trails.android.accessibility.TrailsAccessibilityInstrumentation"
        buildConfigField("boolean", "INITIAL_OFFLINE", initialOfflinePreview.toString())
        if (initialOfflinePreview) applicationId = "org.mobilenativefoundation.trails.android.offlinepreview"
    }
}

dependencies {
    debugImplementation(projects.multiplatform.data.trail.impl)
    debugImplementation(libs.kotlinx.coroutines.core)
    implementation(compose.runtime)
    implementation(compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.activity)
    implementation(libs.androidx.core)
    implementation(libs.androidx.viewmodel)
    implementation(libs.kotlin.stdlib)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.runtime)
    implementation(projects.multiplatform.app.runtime)
    implementation(libs.splashscreen)
    implementation(projects.multiplatform.foundation.designsystem)
}
