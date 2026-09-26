plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.di.graph.loggedout)
                api(projects.multiplatform.di.graph.loggedin)
                api(projects.multiplatform.app.bootstrap.impl)
                api(projects.multiplatform.app.context)
                api(projects.multiplatform.foundation.networking)
                api(projects.multiplatform.foundation.logging)
                implementation(projects.multiplatform.foundation.coroutines)
                api(projects.multiplatform.model.domain)
                api(projects.multiplatform.model.db)
                api(projects.multiplatform.data.trail.impl)
                api(projects.multiplatform.data.user.impl)
                api(projects.multiplatform.data.post.impl)
                api(projects.multiplatform.data.auth.impl)
                api(projects.multiplatform.data.devsettings.impl)
                api(projects.server.api)
                api(projects.server.fake)
            }
        }
    }
}


android {
    namespace = "org.mobilenativefoundation.trails.di.graph.app"
}
