package org.mobilenativefoundation.trails.tooling.plugins

import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.mobilenativefoundation.trails.tooling.extensions.libs

class TrailsFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("plugin.trails.kotlin.android.library")
            apply("plugin.trails.kotlin.multiplatform")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("dev.zacsweers.metro")
            apply("com.google.devtools.ksp")
            apply("org.jetbrains.kotlin.plugin.parcelize")
        }

        extensions.configure<KotlinMultiplatformExtension> {

            targets.configureEach {
                if (platformType == KotlinPlatformType.androidJvm) {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.addAll(
                                    "-P",
                                    "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=org.mobilenativefoundation.trails.foundation.parcel.Parcelize",
                                )
                            }
                        }
                    }
                }
            }

            sourceSets {
                configureEach { compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") } }

                commonMain.dependencies {
                    val kotlinStdLib = libs.findLibrary("kotlin-stdlib").get()
                    val circuitFoundation = libs.findLibrary("circuit-foundation").get()
                    val circuitRuntime = libs.findLibrary("circuit-runtime").get()
                    val composeMultiplatformVersion = libs.findVersion("composeMultiplatform").get()
                    val composeMaterial3Version = libs.findVersion("composeMaterial3").get()
                    implementation("org.jetbrains.compose.runtime:runtime:$composeMultiplatformVersion")
                    implementation("org.jetbrains.compose.material3:material3:$composeMaterial3Version")
                    implementation("org.jetbrains.compose.components:components-resources:$composeMultiplatformVersion")
                    implementation(kotlinStdLib)
                    implementation(circuitFoundation)
                    implementation(circuitRuntime)
                    implementation(project(":multiplatform:foundation:parcel"))
                }
            }
        }

        extensions.configure<MetroPluginExtension> {
            generateAssistedFactories.set(true)
        }
    }
}