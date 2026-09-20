plugins {
    id("plugin.trails.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.di.graph.loggedout)
                api(projects.multiplatform.di.graph.loggedin)
                api(projects.multiplatform.di.graph.inactive)
                api(projects.multiplatform.di.graph.active)
                api(projects.multiplatform.model.domain)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.app.bootstrap.api"
}
