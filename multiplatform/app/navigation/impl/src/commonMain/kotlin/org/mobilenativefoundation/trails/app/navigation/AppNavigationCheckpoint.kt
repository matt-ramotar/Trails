package org.mobilenativefoundation.trails.app.navigation

import kotlinx.serialization.json.*
import org.mobilenativefoundation.trails.data.trail.catalog.TrailQuery

internal data class AppRoute(val name: String, val id: String? = null)

internal data class AppNavigationCheckpoint(
    val root: AppNavigationController.Root = AppNavigationController.Root.EXPLORE,
    val exploreRoutes: List<AppRoute> = listOf(AppRoute("explore")),
    val savedRoutes: List<AppRoute> = listOf(AppRoute("saved")),
    val forYouRoutes: List<AppRoute> = listOf(AppRoute("foryou")),
    val navigateRoutes: List<AppRoute> = listOf(AppRoute("navigate")),
    val activityRoutes: List<AppRoute> = listOf(AppRoute("activity")),
    val lastTrail: String? = null,
    val explore: ExploreViewState = ExploreViewState(),
    val allTrails: Boolean = false,
    val scroll: Map<String, ScrollPosition> = emptyMap(),
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
        fun decode(value: String): AppNavigationCheckpoint {
            require(value.length <= MAX_CHECKPOINT_CHARACTERS) { "Navigation checkpoint is too large" }
            val json = Json.parseToJsonElement(value).jsonObject
            require(json.getValue("version").jsonPrimitive.int == 1) { "Unsupported navigation checkpoint version" }
            val scroll = json.getValue("scroll").jsonObject
            require(scroll.size <= 64)
            // Older checkpoints can omit these roots and still restore.
            fun optionalRoutes(field: String, root: String) = json[field]?.let { decodeRoutes(it, root) } ?: listOf(AppRoute(root))
            return AppNavigationCheckpoint(
                root = AppNavigationController.Root.valueOf(json.getValue("root").jsonPrimitive.content),
                exploreRoutes = decodeRoutes(json.getValue("exploreRoutes"), "explore"),
                savedRoutes = decodeRoutes(json.getValue("savedRoutes"), "saved"),
                forYouRoutes = optionalRoutes("forYouRoutes", "foryou"),
                navigateRoutes = optionalRoutes("navigateRoutes", "navigate"),
                activityRoutes = optionalRoutes("activityRoutes", "activity"),
                lastTrail = json["lastTrail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                explore = ExploreViewState(json.getValue("text").jsonPrimitive.content, Json.decodeFromJsonElement(TrailQuery.serializer(), json.getValue("query")).normalized()),
                allTrails = json.getValue("allTrails").jsonPrimitive.boolean,
                scroll = scroll.mapValues { (_, entry) -> entry.jsonObject.let { ScrollPosition(it.getValue("index").jsonPrimitive.int, it.getValue("offset").jsonPrimitive.int) } },
            )
        }

        private fun encodeRoutes(routes: List<AppRoute>): JsonArray {
            require(routes.size in 1..64)
            return buildJsonArray { routes.forEach { route -> add(buildJsonObject { put("name", route.name); route.id?.let { put("id", it) } }) } }
        }

        private fun decodeRoutes(value: JsonElement, root: String): List<AppRoute> {
            val array = value.jsonArray
            require(array.size in 1..64)
            return array.map { entry ->
                val route = entry.jsonObject
                AppRoute(route.getValue("name").jsonPrimitive.content, route["id"]?.jsonPrimitive?.content)
            }.also { routes ->
                require(routes.first() == AppRoute(root))
                require(routes.drop(1).all { it.name in setOf("trail", "collection") && !it.id.isNullOrBlank() })
                require(root == "saved" || routes.none { it.name == "collection" })
            }
        }
    }
}
