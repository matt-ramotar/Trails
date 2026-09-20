@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store6.core.StoreResult
import org.mobilenativefoundation.trails.server.BackendConfig
import org.mobilenativefoundation.trails.server.NetworkMode

/** Folds Store results into LoadState; cached data stays visible through later failures. */
internal fun <V : Any> Flow<StoreResult<V>>.loadStates(config: StateFlow<BackendConfig?>, onData: () -> Unit = {}): Flow<LoadState<V>> =
    combine(flow {
        var latest = LoadState<V>()
        this@loadStates.collect { result ->
            latest = when (result) {
                is StoreResult.Loading -> latest.copy(loading = true)
                is StoreResult.Data -> { onData(); LoadState(result.value, result.refreshing) }
                is StoreResult.Error -> latest.copy(loading = false, error = result.error.messageText())
                is StoreResult.Revalidated -> latest.copy(loading = false, error = null)
            }
            emit(latest)
        }
    }, config) { state, configured -> state.copy(offline = configured?.networkMode == NetworkMode.OFFLINE) }
