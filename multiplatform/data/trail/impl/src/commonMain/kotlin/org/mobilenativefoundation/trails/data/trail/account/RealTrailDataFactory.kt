@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.trails.data.backend.BackendConfig
import org.mobilenativefoundation.trails.data.trail.catalog.TrailRepository
import org.mobilenativefoundation.trails.data.trail.internal.account.AccountRecoveryPolicy
import org.mobilenativefoundation.trails.data.trail.internal.account.RealTrailAccount
import org.mobilenativefoundation.trails.data.trail.internal.backend.PersistentFakeBackend
import org.mobilenativefoundation.trails.data.trail.internal.backend.SEED_VERSION
import org.mobilenativefoundation.trails.data.trail.internal.catalog.RealTrailRepository
import org.mobilenativefoundation.trails.data.trail.storage.TrailDatabaseDriverFactory
import org.mobilenativefoundation.trails.data.trail.storage.db.TrailDataDatabase

/** The app owns the public catalog and backend, plus one active private account. The account retires independently. */
class RealTrailDataFactory internal constructor(
    private val drivers: TrailDatabaseDriverFactory,
    appScope: CoroutineScope,
    private val recovery: AccountRecoveryPolicy,
) : TrailDataFactory {
    constructor(drivers: TrailDatabaseDriverFactory, appScope: CoroutineScope) : this(drivers, appScope, AccountRecoveryPolicy())
    private val lifecycle = Mutex()
    private val job = SupervisorJob(appScope.coroutineContext[Job])
    private val scope = CoroutineScope(appScope.coroutineContext + job)
    // File names are an installed-data contract. Retain them across source and package renames.
    private val backendDriver = drivers.open("trails-m1-backend.db")
    private val backend = PersistentFakeBackend(TrailDataDatabase(backendDriver))
    private val catalogDriver = drivers.open("trails-m1-catalog.db")
    private val catalogDatabase = TrailDataDatabase(catalogDriver)
    init {
        // This database has its own otherwise-unused backend_meta row. Reuse its seed field
        // as a catalog marker so a restart between the two database commits still clears
        // old pages. Clearing and marking happen before any Store observers or leases exist.
        catalogDatabase.transaction {
            val queries = catalogDatabase.trailDataQueries
            queries.initializeBackend("{}")
            if (backend.reseeded || queries.backendMeta().executeAsOne().seeded != SEED_VERSION) {
                queries.deleteAllCache()
                queries.setSeeded(SEED_VERSION)
            }
        }
    }
    private val catalog = RealTrailRepository(catalogDriver, catalogDatabase, backend)
    private var current: RealTrailAccount? = null
    private var closed = false
    private var catalogDriverClosed = false
    private var backendDriverClosed = false
    override val trails: TrailRepository = catalog
    override val backendConfig = backend.config

    override suspend fun restoreBackendConfig(): BackendConfig = backend.restore()
    override suspend fun applyBackendConfig(config: BackendConfig) { backend.apply(config) }
    override suspend fun open(accountId: String): TrailAccount = lifecycle.withLock {
        check(!closed)
        backend.ensureReady()
        require(accountId.isNotBlank())
        current?.takeIf { it.accountId == accountId && !it.isClosed }?.let { return@withLock it }
        current?.close()
        // Hex-encoded account bytes prevent path separators or identities colliding in file names.
        val partition = accountId.encodeToByteArray().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val values = drivers.open("trails-m1-$partition-values.db")
        val journal = try { drivers.open("trails-m1-$partition-journal.db") } catch (failure: Throwable) { values.close(); throw failure }
        val account = try { RealTrailAccount(accountId, values, journal, backend, catalog, scope, recovery) }
        catch (failure: Throwable) { values.close(); journal.close(); throw failure }
        try { account.initialize() } catch (failure: Throwable) { account.close(); throw failure }
        current = account
        account
    }
    override suspend fun close() = lifecycle.withLock {
        if (!catalogDriverClosed || !backendDriverClosed) {
            closed = true
            current?.close()
            current = null
            job.cancelAndJoin()
            catalog.close()
            backend.close()
            if (!catalogDriverClosed) { catalogDriver.close(); catalogDriverClosed = true }
            if (!backendDriverClosed) { backendDriver.close(); backendDriverClosed = true }
        }
    }

    /** Inspects the durable fake server, not a process counter. */
    suspend fun backendEvidence(): BackendEvidence = backend.evidence()
    suspend fun loseNextAcknowledgement() { backend.loseNextAcknowledgement() }
}
