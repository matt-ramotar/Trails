package org.mobilenativefoundation.trails.server.fake.services

import org.mobilenativefoundation.trails.server.BackendConfigProvider
import org.mobilenativefoundation.trails.server.error.ServerError
import org.mobilenativefoundation.trails.server.fake.internal.seed.SeedDataLoader
import org.mobilenativefoundation.trails.server.fake.internal.simulation.ErrorSimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.LatencySimulator
import org.mobilenativefoundation.trails.server.fake.internal.simulation.NetworkGate
import org.mobilenativefoundation.trails.server.fake.internal.storage.BackendTables

internal open class BaseFakeService(
    protected val tables: BackendTables,
    private val networkGate: NetworkGate,
    private val latencySimulator: LatencySimulator,
    private val errorSimulator: ErrorSimulator,
    private val seedLoader: SeedDataLoader,
    private val configProvider: BackendConfigProvider,
) {
    protected suspend fun <T> withSimulation(block: suspend () -> T): T {
        networkGate.ensureOnline()?.let { throw it }
        errorSimulator.shouldError()?.let { throw it }
        seedLoader.ensureSeeded()
        return latencySimulator.withLatency { block() }
    }

    protected fun requireNotNull(value: Any?, error: ServerError): Nothing? {
        if (value == null) throw error
        return null
    }
}
