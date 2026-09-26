import java.util.Properties

plugins {
    kotlin("multiplatform") version "2.3.20"
    kotlin("android") version "2.3.20" apply false
    kotlin("plugin.compose") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    kotlin("plugin.parcelize") version "2.3.20" apply false
    id("com.android.library") version "8.12.3"
    id("com.android.application") version "8.12.3" apply false
    id("org.jetbrains.compose") version "1.9.1"
    id("dev.zacsweers.metro") version "0.11.3"
    id("com.google.devtools.ksp") version "2.3.10"
    id("app.cash.sqldelight") version "2.1.0"
}

// Load the dependency versions from the hash-verified local candidate.
val candidate = Properties().apply {
    rootProject.file("candidate.properties").inputStream().use(::load)
}
val atomVersion = requireNotNull(candidate.getProperty("atomVersion"))

val verifyCandidate = tasks.register("verifyCandidate") {
    doLast {
        val process = ProcessBuilder("python3", rootProject.file("prepare.py").absolutePath, "--check-candidate")
            .directory(rootDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "Dependency artifact verification failed before compilation:\n$output" }
    }
}
allprojects {
    tasks.configureEach {
        if (name.startsWith("compile")) dependsOn(verifyCandidate)
    }
}

kotlin {
    androidTarget()
    jvm()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            implementation("org.mobilenativefoundation.store:core:6.0.0-SNAPSHOT")
            implementation("org.mobilenativefoundation.store:sqldelight:6.0.0-SNAPSHOT")
            implementation("org.mobilenativefoundation.store:mutations:6.0.0-SNAPSHOT")
            implementation("org.mobilenativefoundation.store:mutations-sqldelight:6.0.0-SNAPSHOT")
            implementation("dev.mattramotar.atom:core:$atomVersion")
            implementation("dev.mattramotar.atom:compose:$atomVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("app.cash.sqldelight:runtime:2.1.0")
            implementation("com.slack.circuit:circuit-runtime:0.30.0")
            implementation("com.slack.circuit:circuit-runtime-presenter:0.30.0")
            implementation("com.slack.circuit:circuit-runtime-ui:0.30.0")
            implementation("dev.zacsweers.metro:runtime:0.11.3")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        androidMain.dependencies { implementation("app.cash.sqldelight:android-driver:2.1.0") }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.integration"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("FixtureDatabase") {
            packageName.set("org.mobilenativefoundation.trails.integration.db")
        }
    }
}
