pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val candidateCheck = ProcessBuilder("python3", file("prepare.py").absolutePath, "--check-candidate")
    .directory(rootDir).redirectErrorStream(true).start()
val candidateCheckOutput = candidateCheck.inputStream.bufferedReader().readText()
require(candidateCheck.waitFor() == 0) { "C3 candidate verification failed:\n$candidateCheckOutput" }

val candidateFile = file("candidate.properties")
require(candidateFile.isFile) {
    "C3 is not prepared. Supply the clean Atom handoff and isolated artifact repository to prepare.py."
}
val candidate = java.util.Properties().apply { candidateFile.inputStream().use(::load) }
val isolatedRepository = candidate.getProperty("isolatedMavenRepository")
    ?: error("The prepared candidate must name its isolated Maven repository.")
val repositoryPath = java.nio.file.Path.of(isolatedRepository).toRealPath()
require(repositoryPath != java.nio.file.Path.of(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath()) {
    "Ambient Maven Local is not a C3 source."
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri(repositoryPath.toFile()) } }
            filter {
                includeGroup("org.mobilenativefoundation.store")
                includeGroup("dev.mattramotar.atom")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "trails-store6-consumer"
include(":android-preview")
