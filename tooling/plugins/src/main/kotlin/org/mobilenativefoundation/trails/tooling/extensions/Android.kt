package org.mobilenativefoundation.trails.tooling.extensions

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureAndroid() {
    val versions = getVersions()

    android {

        compileSdkVersion(versions.compileSdk)

        defaultConfig {
            minSdk = versions.minSdk
            targetSdk = versions.targetSdk
        }
    }
}

fun Project.android(action: BaseExtension.() -> Unit) = extensions.configure<BaseExtension>(action)

