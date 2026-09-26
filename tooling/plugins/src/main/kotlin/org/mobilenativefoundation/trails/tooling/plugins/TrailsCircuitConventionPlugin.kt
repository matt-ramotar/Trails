package org.mobilenativefoundation.trails.tooling.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.mobilenativefoundation.trails.tooling.extensions.libs

class TrailsCircuitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("plugin.trails.compose")
        pluginManager.apply("plugin.trails.di")
        pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")
        extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach {
                if (platformType == KotlinPlatformType.androidJvm) compilations.configureEach {
                    compileTaskProvider.configure {
                        compilerOptions.freeCompilerArgs.addAll(
                            "-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=org.mobilenativefoundation.trails.foundation.parcel.Parcelize",
                        )
                    }
                }
            }
            sourceSets.commonMain.dependencies {
                implementation(libs.findLibrary("circuit-foundation").get())
                api(libs.findLibrary("circuit-runtime").get())
                api(libs.findLibrary("circuit-runtime-ui").get())
                api(libs.findLibrary("circuit-runtime-presenter").get())
                api(project(":multiplatform:foundation:parcel"))
            }
        }
    }
}
