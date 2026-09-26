package org.mobilenativefoundation.trails.app.runtime

import androidx.compose.runtime.Composable
import org.mobilenativefoundation.trails.app.runtime.graph.app.AppGraph
import org.mobilenativefoundation.trails.data.session.UserRepository
import org.mobilenativefoundation.trails.data.trail.account.TrailDataFactory

/** Host entry point for rendering Trails and inspecting its running services. */
class TrailsRuntime internal constructor(
    private val graph: AppGraph,
    private val className: (Any) -> String = { it::class.simpleName ?: "Unknown" },
) {
    private val controller = MainViewController(graph)

    val isReady: Boolean get() = graph.splashStateReader.state.value.isReady()
    val userRepository: UserRepository get() = graph.userRepository
    val trailData: TrailDataFactory get() = graph.trailData

    @Composable
    fun Content() = controller.Content()

    /** Reads current state without starting, retrying, or replacing any service. */
    fun diagnosticSnapshot(): Map<String, String> = mapOf(
        "bootstrapRouteClass" to className(graph.bootstrapCoordinator.route.value),
        "backendConfigSync" to graph.backendConfigSynchronizer.status.value.toString(),
        "userClass" to className(userRepository.current),
        "backendConfig" to trailData.backendConfig.value.toString(),
    )
}
