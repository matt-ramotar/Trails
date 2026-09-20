plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.model.domain)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.di.graph.inactive"
}
