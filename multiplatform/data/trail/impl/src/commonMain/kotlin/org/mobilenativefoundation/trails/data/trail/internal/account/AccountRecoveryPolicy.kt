@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.internal.account

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeoutOrNull
import org.mobilenativefoundation.store6.core.seam.WallClock

/** Recovery passes use the durable journal and Store6 drainer even with custom scheduling. */
internal class AccountRecoveryPolicy(
    val automatic: Boolean = true,
    val clock: WallClock? = null,
    val awaitRetry: suspend (ReceiveChannel<Unit>) -> Unit = { wake ->
        withTimeoutOrNull(30_000) { wake.receive() }
        Unit
    },
)
