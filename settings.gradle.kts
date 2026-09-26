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

// Only immutable, hash-verified local publications may provide the M1 dependencies.
val candidateDirectory = file("integration-tests/store6-consumer")
val candidateCheck = providers.exec {
    commandLine("python3", File(candidateDirectory, "prepare.py").absolutePath, "--check-candidate", "--trails-targets")
    workingDir(candidateDirectory)
    isIgnoreExitValue = true
}
require(candidateCheck.result.get().exitValue == 0) {
    "M1 dependency provenance check failed:\n${candidateCheck.standardOutput.asText.get()}${candidateCheck.standardError.asText.get()}"
}
val candidate = java.util.Properties().apply {
    File(candidateDirectory, "candidate.properties").inputStream().use(::load)
}
val isolatedMavenRepository = candidate.getProperty("isolatedMavenRepository")
    ?: error("Prepare the immutable M1 dependencies first; see docs/store6-integration.md.")

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

include(":server:api")
include(":server:fake")

include(":multiplatform:foundation:parcel")
include(":multiplatform:di:scope")
include(":multiplatform:di:graph:active")
include(":multiplatform:di:graph:inactive")
include(":multiplatform:di:graph:loggedin")
include(":multiplatform:di:graph:app")
include(":multiplatform:app:core")
include(":multiplatform:app:context")
include(":multiplatform:di:graph:loggedout")
include(":multiplatform:model:network")
include(":multiplatform:model:domain")
include(":multiplatform:model:db")
include(":multiplatform:app:bootstrap:impl")
include(":multiplatform:app:bootstrap:api")
include(":multiplatform:foundation:designsystem")
include(":multiplatform:foundation:networking")
include(":multiplatform:foundation:logging")
include(":multiplatform:foundation:coroutines")
include(":multiplatform:data:user:api")
include(":multiplatform:data:user:impl")
include(":multiplatform:data:post:api")
include(":multiplatform:data:post:impl")
include(":multiplatform:data:devsettings:api")
include(":multiplatform:data:devsettings:impl")
include(":multiplatform:data:auth:api")
include(":multiplatform:data:auth:impl")

include(":multiplatform:screen:home:api")
include(":multiplatform:screen:home:impl")

include(":multiplatform:screen:login:api")
include(":multiplatform:screen:login:impl")

include(":multiplatform:screen:prelanding:api")
include(":multiplatform:screen:prelanding:impl")

include(":multiplatform:screen:signup:api")
include(":multiplatform:screen:signup:impl")

include(":multiplatform:screen:welcome:api")
include(":multiplatform:screen:welcome:impl")

include(":multiplatform:screen:profile:api")
include(":multiplatform:screen:profile:impl")

include(":multiplatform:app:scaffold:api")
include(":multiplatform:app:scaffold:impl")

include(":apps:android")


include(":multiplatform:data:trail:api")
include(":multiplatform:data:trail:impl")
include(":multiplatform:screen:explore:api")
include(":multiplatform:screen:explore:impl")
include(":multiplatform:screen:traildetail:api")
include(":multiplatform:screen:traildetail:impl")
include(":multiplatform:screen:saved:api")
include(":multiplatform:screen:saved:impl")
include(":multiplatform:screen:collection:api")
include(":multiplatform:screen:collection:impl")
include(":multiplatform:screen:navigate:api")
include(":multiplatform:screen:navigate:impl")
include(":multiplatform:screen:activity:api")
include(":multiplatform:screen:activity:impl")
include(":multiplatform:screen:foryou:api")
include(":multiplatform:screen:foryou:impl")
include(":multiplatform:feat:filters:api")
include(":multiplatform:feat:filters:impl")
include(":multiplatform:feat:savetrail:api")
include(":multiplatform:feat:savetrail:impl")
