plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.model.domain)
                api(projects.multiplatform.screen.welcome.impl)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.di.graph.loggedout"
}
