package org.mobilenativefoundation.trails.app

import org.mobilenativefoundation.trails.di.graph.app.AppGraph

expect class TrailsApp {
    val graph: AppGraph
}

