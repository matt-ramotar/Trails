plugins {
    id("plugin.trails.circuit")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.multiplatform.foundation.scope)
            implementation(projects.multiplatform.foundation.coroutines)
            implementation(projects.multiplatform.foundation.logging)
            api(projects.multiplatform.foundation.designsystem)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            api(projects.multiplatform.data.session.api)
            implementation(projects.multiplatform.data.session.impl)
            api(projects.multiplatform.data.trail.api)
            implementation(projects.multiplatform.data.trail.impl)
            implementation(projects.multiplatform.data.developersettings.impl)
            implementation(projects.multiplatform.data.database)
            implementation(projects.multiplatform.app.navigation.impl)
            implementation(projects.multiplatform.screen.explore.impl)
            implementation(projects.multiplatform.screen.saved.impl)
            implementation(projects.multiplatform.screen.traildetail.impl)
            implementation(projects.multiplatform.screen.collection.impl)
            implementation(projects.multiplatform.screen.navigate.impl)
            implementation(projects.multiplatform.screen.activity.impl)
            implementation(projects.multiplatform.screen.foryou.impl)
            implementation(projects.multiplatform.screen.welcome.impl)
            implementation(projects.multiplatform.screen.prelanding.api)
            implementation(projects.multiplatform.feature.filters.impl)
            implementation(projects.multiplatform.feature.savetrail.impl)
            implementation(projects.multiplatform.feature.developertools.impl)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.compose.activity)
            implementation(libs.androidx.core)
            implementation(libs.androidx.viewmodel)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.sqldelight.jvm)
            runtimeOnly(compose.desktop.currentOs)
        }
    }
}

android { namespace = "org.mobilenativefoundation.trails.app.runtime" }
