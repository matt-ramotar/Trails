package org.mobilenativefoundation.trails.foundation.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrailRouteSchematicTest {
    @Test
    fun routesAreDeterministicBoundedAndLoopsClose() {
        val loop = schematicRoute("half-dome", loop = true)
        assertEquals(loop, schematicRoute("half-dome", loop = true))
        assertEquals(15, loop.size)
        assertEquals(loop.first(), loop.last())
        val line = schematicRoute("trolltunga", loop = false)
        assertEquals(14, line.size)
        assertTrue(line.zipWithNext().all { (a, b) -> b.first > a.first })
        (loop + line).forEach { (x, y) ->
            assertTrue(x in 0.05f..0.95f, "x=$x")
            assertTrue(y in 0.05f..0.95f, "y=$y")
        }
        assertTrue(schematicRoute("a", loop = true) != schematicRoute("b", loop = true))
    }
}
