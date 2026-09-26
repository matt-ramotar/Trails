plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.model.domain)
                api(projects.multiplatform.di.graph.active)
                api(projects.multiplatform.di.graph.inactive)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.di.graph.loggedin"
}
