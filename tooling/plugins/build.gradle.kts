plugins {
    `kotlin-dsl`
}

group = "org.mobilenativefoundation.trails.tooling"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    compileOnly(libs.metro.gradle.plugin)
}

gradlePlugin {
    plugins {

        register("kotlinAndroidLibraryPlugin") {
            id = "plugin.trails.kotlin.android.library"
            implementationClass =
                "org.mobilenativefoundation.trails.tooling.plugins.KotlinAndroidLibraryConventionPlugin"
        }

        register("kotlinMultiplatformPlugin") {
            id = "plugin.trails.kotlin.multiplatform"
            implementationClass =
                "org.mobilenativefoundation.trails.tooling.plugins.KotlinMultiplatformConventionPlugin"
        }

        register("androidApplicationPlugin") {
            id = "plugin.trails.android.application"
            implementationClass = "org.mobilenativefoundation.trails.tooling.plugins.AndroidApplicationConventionPlugin"
        }

        register("trailsFeaturePlugin") {
            id = "plugin.trails.feature"
            implementationClass = "org.mobilenativefoundation.trails.tooling.plugins.TrailsFeatureConventionPlugin"
        }

        register("trailsLibraryPlugin") {
            id = "plugin.trails.library"
            implementationClass = "org.mobilenativefoundation.trails.tooling.plugins.TrailsLibraryConventionPlugin"
        }
    }
}
