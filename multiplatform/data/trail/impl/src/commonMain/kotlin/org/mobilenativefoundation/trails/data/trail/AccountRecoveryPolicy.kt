@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeoutOrNull
import org.mobilenativefoundation.store6.core.seam.WallClock

/** Internal scheduling seam: each pass still uses the durable journal and Store6 drainer. */
internal class AccountRecoveryPolicy(
    val automatic: Boolean = true,
    val clock: WallClock? = null,
    val awaitRetry: suspend (ReceiveChannel<Unit>) -> Unit = { wake ->
        withTimeoutOrNull(30_000) { wake.receive() }
        Unit
    },
)
