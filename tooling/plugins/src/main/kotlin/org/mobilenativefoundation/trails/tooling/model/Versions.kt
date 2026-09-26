package org.mobilenativefoundation.trails.tooling.model

import org.gradle.api.JavaVersion

data class Versions(
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int,
    val javaVersion: JavaVersion
)
