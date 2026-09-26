plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                implementation(libs.kotlinx.serialization.json)
                api(projects.multiplatform.data.trail.api)
                api(projects.multiplatform.screen.explore.impl)
                api(projects.multiplatform.screen.traildetail.impl)
                api(projects.multiplatform.screen.saved.impl)
                api(projects.multiplatform.screen.collection.impl)
                api(projects.multiplatform.screen.navigate.impl)
                api(projects.multiplatform.screen.activity.impl)
                api(projects.multiplatform.screen.foryou.impl)
                api(projects.multiplatform.feat.filters.impl)
                api(projects.multiplatform.feat.savetrail.impl)
                api(projects.multiplatform.model.domain)
                api(projects.multiplatform.app.scaffold.impl)
                api(projects.multiplatform.screen.home.impl)
                api(projects.multiplatform.screen.profile.impl)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.di.graph.active"
}
