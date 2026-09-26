plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.multiplatform.app.navigation.api)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.multiplatform.screen.explore.api)
            implementation(projects.multiplatform.screen.saved.api)
            implementation(projects.multiplatform.screen.traildetail.api)
            implementation(projects.multiplatform.screen.collection.api)
            implementation(projects.multiplatform.screen.navigate.api)
            implementation(projects.multiplatform.screen.activity.api)
            implementation(projects.multiplatform.screen.foryou.api)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.app.navigation.impl" }
