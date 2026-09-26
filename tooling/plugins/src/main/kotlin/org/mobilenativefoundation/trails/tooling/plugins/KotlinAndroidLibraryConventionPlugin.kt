package org.mobilenativefoundation.trails.tooling.plugins

import com.android.build.gradle.LibraryExtension
import org.mobilenativefoundation.trails.tooling.extensions.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.mobilenativefoundation.trails.tooling.extensions.getVersions

class KotlinAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val versions = target.getVersions()
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            configureKotlin()

            extensions.configure<LibraryExtension> {
                compileSdk = versions.compileSdk

                defaultConfig {
                    minSdk = versions.minSdk
                    targetSdk = versions.targetSdk
                    multiDexEnabled = true
                }
            }
        }
    }
}