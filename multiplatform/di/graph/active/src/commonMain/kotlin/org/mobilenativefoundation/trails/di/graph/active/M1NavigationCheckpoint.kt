package org.mobilenativefoundation.trails.di.graph.active

import kotlinx.serialization.json.*
import org.mobilenativefoundation.trails.data.trail.TrailQuery
import org.mobilenativefoundation.trails.screen.explore.M1ExploreView
import org.mobilenativefoundation.trails.screen.explore.M1ScrollPosition

internal data class M1Route(val name: String, val id: String? = null)

internal data class M1NavigationCheckpoint(
    val root: M1NavigationController.Root = M1NavigationController.Root.EXPLORE,
    val exploreRoutes: List<M1Route> = listOf(M1Route("explore")),
    val savedRoutes: List<M1Route> = listOf(M1Route("saved")),
    val forYouRoutes: List<M1Route> = listOf(M1Route("foryou")),
    val navigateRoutes: List<M1Route> = listOf(M1Route("navigate")),
    val activityRoutes: List<M1Route> = listOf(M1Route("activity")),
    val lastTrail: String? = null,
    val explore: M1ExploreView = M1ExploreView(),
    val allTrails: Boolean = false,
    val scroll: Map<String, M1ScrollPosition> = emptyMap(),
) {
    fun encode(): String = buildJsonObject {
        put("version", 1)
        put("root", root.name)
        put("exploreRoutes", encodeRoutes(exploreRoutes))
        put("savedRoutes", encodeRoutes(savedRoutes))
        put("forYouRoutes", encodeRoutes(forYouRoutes))
        put("navigateRoutes", encodeRoutes(navigateRoutes))
        put("activityRoutes", encodeRoutes(activityRoutes))
        lastTrail?.let { put("lastTrail", it) }
        put("text", explore.text)
        put("query", Json.encodeToJsonElement(TrailQuery.serializer(), explore.query))
        put("allTrails", allTrails)
        putJsonObject("scroll") {
            scroll.forEach { (key, value) -> putJsonObject(key) { put("index", value.index); put("offset", value.offset) } }
        }
    }.toString().also { require(it.length <= MAX_CHECKPOINT_CHARACTERS) { "Navigation checkpoint is too large" } }

    companion object {
        private const val MAX_CHECKPOINT_CHARACTERS = 128_000
        fun decode(value: String): M1NavigationCheckpoint {
            require(value.length <= MAX_CHECKPOINT_CHARACTERS) { "Navigation checkpoint is too large" }
            val json = Json.parseToJsonElement(value).jsonObject
            require(json.getValue("version").jsonPrimitive.int == 1) { "Unsupported navigation checkpoint version" }
            val scroll = json.getValue("scroll").jsonObject
            require(scroll.size <= 64)
            // Roots added after the first checkpoints are optional so earlier snapshots still restore.
            fun optionalRoutes(field: String, root: String) = json[field]?.let { decodeRoutes(it, root) } ?: listOf(M1Route(root))
            return M1NavigationCheckpoint(
                root = M1NavigationController.Root.valueOf(json.getValue("root").jsonPrimitive.content),
                exploreRoutes = decodeRoutes(json.getValue("exploreRoutes"), "explore"),
                savedRoutes = decodeRoutes(json.getValue("savedRoutes"), "saved"),
                forYouRoutes = optionalRoutes("forYouRoutes", "foryou"),
                navigateRoutes = optionalRoutes("navigateRoutes", "navigate"),
                activityRoutes = optionalRoutes("activityRoutes", "activity"),
                lastTrail = json["lastTrail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                explore = M1ExploreView(json.getValue("text").jsonPrimitive.content, Json.decodeFromJsonElement(TrailQuery.serializer(), json.getValue("query")).normalized()),
                allTrails = json.getValue("allTrails").jsonPrimitive.boolean,
                scroll = scroll.mapValues { (_, entry) -> entry.jsonObject.let { M1ScrollPosition(it.getValue("index").jsonPrimitive.int, it.getValue("offset").jsonPrimitive.int) } },
            )
        }

        private fun encodeRoutes(routes: List<M1Route>): JsonArray {
            require(routes.size in 1..64)
            return buildJsonArray { routes.forEach { route -> add(buildJsonObject { put("name", route.name); route.id?.let { put("id", it) } }) } }
        }

        private fun decodeRoutes(value: JsonElement, root: String): List<M1Route> {
            val array = value.jsonArray
            require(array.size in 1..64)
            return array.map { entry ->
                val route = entry.jsonObject
                M1Route(route.getValue("name").jsonPrimitive.content, route["id"]?.jsonPrimitive?.content)
            }.also { routes ->
                require(routes.first() == M1Route(root))
                require(routes.drop(1).all { it.name in setOf("trail", "collection") && !it.id.isNullOrBlank() })
                require(root == "saved" || routes.none { it.name == "collection" })
            }
        }
    }
}
