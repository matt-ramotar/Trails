package org.mobilenativefoundation.trails.tooling.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.mobilenativefoundation.trails.tooling.extensions.libs

class TrailsLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("plugin.trails.kotlin.android.library")
            apply("plugin.trails.kotlin.multiplatform")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                configureEach { compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") } }

                commonMain.dependencies {
                    val kotlinStdLib = libs.findLibrary("kotlin-stdlib").get()
                    implementation(kotlinStdLib)
                }
            }
        }
    }
}