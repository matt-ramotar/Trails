package org.mobilenativefoundation.trails.tooling.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.mobilenativefoundation.trails.tooling.extensions.libs

class TrailsComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("plugin.trails.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                val composeVersion = libs.findVersion("composeMultiplatform").get()
                val materialVersion = libs.findVersion("composeMaterial3").get()
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                implementation("org.jetbrains.compose.material3:material3:$materialVersion")
                implementation("org.jetbrains.compose.components:components-resources:$composeVersion")
            }
        }
    }
}
