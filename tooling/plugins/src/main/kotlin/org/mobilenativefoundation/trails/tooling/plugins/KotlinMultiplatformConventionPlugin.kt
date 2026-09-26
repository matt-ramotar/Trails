package org.mobilenativefoundation.trails.tooling.plugins

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.mobilenativefoundation.trails.tooling.extensions.configureKotlin
import org.mobilenativefoundation.trails.tooling.extensions.libs


class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.kotlinx.kover")
        }

        version = libs.findVersion("trails")

        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()

            if (pluginManager.hasPlugin("com.android.library")) {
                androidTarget()
            }

            jvm()

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            js {
                browser()
                compilerOptions {
                    // Metro reads the compiler flag independently of Gradle's JS IC task settings.
                    // Pin it to false for Kotlin 2.3.20 (KT-82395, KT-82989).
                    freeCompilerArgs.add("-Xenable-incremental-compilation=false")
                }
            }

            targets.all {
                compilations.all {
                    compilerOptions.configure {
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                    }
                }
            }

            sourceSets.commonTest.dependencies {
                val coroutinesTest = libs.findLibrary("kotlinx-coroutines-test").get()
                val kotlinTest = libs.findLibrary("kotlin-test").get()
                val turbine = libs.findLibrary("turbine").get()

                implementation(coroutinesTest)
                implementation(kotlinTest)
                implementation(turbine)
            }

            targets.withType<KotlinNativeTarget>().configureEach {
                compilations.configureEach {
                    compilerOptions.configure {
                        freeCompilerArgs.add("-Xallocator=custom")
                        freeCompilerArgs.add("-Xadd-light-debug=enable")

                        freeCompilerArgs.addAll(
                            "-opt-in=kotlin.RequiresOptIn",
                            "-opt-in=kotlin.time.ExperimentalTime",
                            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                            "-opt-in=kotlinx.coroutines.FlowPreview",
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlinx.cinterop.BetaInteropApi",
                        )
                    }
                }
            }

            configureKotlin()
        }

        extensions.configure<KoverProjectExtension> {
            reports {
                total {
                    xml {
                        onCheck.set(true)
                        xmlFile.set(target.layout.buildDirectory.file("reports/kover/coverage.xml"))
                    }
                }
            }
        }
    }
}
