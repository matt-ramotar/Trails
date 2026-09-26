package org.mobilenativefoundation.trails.tooling.plugins

import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class TrailsDiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.zacsweers.metro")
        pluginManager.apply("com.google.devtools.ksp")
        extensions.configure<MetroPluginExtension> { generateAssistedFactories.set(true) }
    }
}
