package org.mobilenativefoundation.trails.tooling.extensions

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.mobilenativefoundation.trails.tooling.model.Versions

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.getVersions(): Versions {
    val minSdkVersion = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
    val targetSdkVersion = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()
    val compileSdkVersion = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
    return Versions(
        minSdkVersion,
        targetSdkVersion,
        compileSdkVersion,
        JavaVersion.VERSION_17,
    )
}
