package org.mobilenativefoundation.trails.tooling.plugins

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.mobilenativefoundation.trails.tooling.extensions.configureAndroid
import org.mobilenativefoundation.trails.tooling.extensions.configureKotlin
import org.mobilenativefoundation.trails.tooling.extensions.getVersions

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val versions = target.getVersions()
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            configureKotlin()

            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    targetSdk = versions.targetSdk
                    compileSdk = versions.compileSdk
                }

                compileOptions {
                    sourceCompatibility = versions.javaVersion
                    targetCompatibility = versions.javaVersion
                }

                buildFeatures {
                    buildConfig = true
                }

                configureAndroid()
            }
        }
    }
}
