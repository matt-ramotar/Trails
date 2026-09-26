enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("tooling")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Only immutable, hash-verified local publications may provide the Store6 and Atom dependencies.
val candidateDirectory = file("integration-tests/store6-consumer")
val candidateCheck = providers.exec {
    commandLine("python3", File(candidateDirectory, "prepare.py").absolutePath, "--check-candidate", "--trails-targets")
    workingDir(candidateDirectory)
    isIgnoreExitValue = true
}
require(candidateCheck.result.get().exitValue == 0) {
    "Dependency provenance check failed:\n${candidateCheck.standardOutput.asText.get()}${candidateCheck.standardError.asText.get()}"
}
val candidate = java.util.Properties().apply {
    File(candidateDirectory, "candidate.properties").inputStream().use(::load)
}
val isolatedMavenRepository = candidate.getProperty("isolatedMavenRepository")
    ?: error("Prepare the immutable Store6 and Atom dependencies first; see docs/dependency-setup.md.")

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri(isolatedMavenRepository) } }
            filter {
                listOf("core", "sqldelight", "mutations", "mutations-sqldelight").forEach {
                    includeModule("org.mobilenativefoundation.store", it)
                    includeModuleByRegex("org\\.mobilenativefoundation\\.store", "$it-.*")
                }
                listOf("core", "compose").forEach {
                    includeModule("dev.mattramotar.atom", it)
                    includeModuleByRegex("dev\\.mattramotar\\.atom", "$it-.*")
                }
            }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}
rootProject.name = "trails"

include(":apps:android")

include(":multiplatform:app:runtime")
include(":multiplatform:app:navigation:api")
include(":multiplatform:app:navigation:impl")
include(":multiplatform:data:session:api")
include(":multiplatform:data:session:impl")
include(":multiplatform:data:trail:api")
include(":multiplatform:data:trail:impl")
include(":multiplatform:data:developersettings:api")
include(":multiplatform:data:developersettings:impl")
include(":multiplatform:data:backend")
include(":multiplatform:data:database")
include(":multiplatform:ui:trail")
include(":multiplatform:foundation:scope")
include(":multiplatform:foundation:parcel")
include(":multiplatform:foundation:designsystem")
include(":multiplatform:foundation:logging")
include(":multiplatform:foundation:coroutines")
include(":multiplatform:screen:explore:api")
include(":multiplatform:screen:explore:impl")
include(":multiplatform:screen:saved:api")
include(":multiplatform:screen:saved:impl")
include(":multiplatform:screen:traildetail:api")
include(":multiplatform:screen:traildetail:impl")
include(":multiplatform:screen:collection:api")
include(":multiplatform:screen:collection:impl")
include(":multiplatform:screen:navigate:api")
include(":multiplatform:screen:navigate:impl")
include(":multiplatform:screen:activity:api")
include(":multiplatform:screen:activity:impl")
include(":multiplatform:screen:foryou:api")
include(":multiplatform:screen:foryou:impl")
include(":multiplatform:screen:welcome:api")
include(":multiplatform:screen:welcome:impl")
include(":multiplatform:screen:prelanding:api")
include(":multiplatform:feature:filters:api")
include(":multiplatform:feature:filters:impl")
include(":multiplatform:feature:savetrail:api")
include(":multiplatform:feature:savetrail:impl")
include(":multiplatform:feature:developertools:api")
include(":multiplatform:feature:developertools:impl")
