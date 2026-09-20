@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.trail

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.store6.core.StoreMeta
import org.mobilenativefoundation.store6.mutations.sqldelight.SqlDelightMutationJournalStorage
import org.mobilenativefoundation.store6.sqldelight.SqlDelightBookkeeper
import org.mobilenativefoundation.store6.sqldelight.SqlDelightSourceOfTruth
import org.mobilenativefoundation.trails.data.trail.db.M1Database

class StorageLifetimeTest {
    @Test
    fun retirementWaitsForHeldReaderAndCancellationReleasesItsLease() = runTest {
        val lifetime = StorageLifetime()
        val entered = CompletableDeferred<Unit>()
        var readerStopped = false
        var lateCalls = 0
        val reader = launch(start = CoroutineStart.UNDISPATCHED) {
            lifetime.reader {
                flow {
                    try {
                        emit(1)
                        entered.complete(Unit)
                        awaitCancellation()
                    } finally { readerStopped = true }
                }
            }.collect()
        }
        entered.await()
        val retirement = async(start = CoroutineStart.UNDISPATCHED) { lifetime.retire() }
        assertFalse(retirement.isCompleted, "An active upstream reader still owns storage")
        withContext(NonCancellable) {
            assertFailsWith<CancellationException> { lifetime.use { lateCalls++ } }
        }
        assertEquals(0, lateCalls)
        reader.cancelAndJoin()
        retirement.await()
        assertTrue(readerStopped)
    }

    @Test
    fun nestedAndConcurrentOperationsAllFinishBeforeRetirement() = runTest {
        val lifetime = StorageLifetime()
        val firstRelease = CompletableDeferred<Unit>()
        val secondRelease = CompletableDeferred<Unit>()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            lifetime.use { lifetime.use { firstRelease.await(); "first" } }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            lifetime.use { secondRelease.await(); "second" }
        }
        val retirement = async(start = CoroutineStart.UNDISPATCHED) { lifetime.retire() }
        assertFalse(retirement.isCompleted)
        firstRelease.complete(Unit)
        assertEquals("first", first.await())
        assertFalse(retirement.isCompleted, "The independent second operation still owns its lease")
        secondRelease.complete(Unit)
        assertEquals("second", second.await())
        retirement.await()
        lifetime.retire() // Repeated retirement is safe.
    }

    @Test
    fun failedOperationReleasesItsLeaseWithoutChangingTheFailure() = runTest {
        val lifetime = StorageLifetime()
        val failure = assertFailsWith<IllegalStateException> {
            lifetime.use<Unit> { error("Injected storage failure") }
        }
        assertEquals("Injected storage failure", failure.message)
        withTimeout(1_000) { lifetime.retire() }
    }

    @Test
    fun realJournalTransactionCommitsBeforeRetirementClosesItsDriver() = runBlocking {
        val directory = Files.createTempDirectory("trails-storage-lifetime").toFile()
        val factory = PlatformM1DriverFactory(directory)
        val underlying = factory.open("transaction.db")
        val closeCalls = AtomicInteger()
        val driver = object : SqlDriver by underlying {
            override fun close() { closeCalls.incrementAndGet(); underlying.close() }
        }
        val database = M1Database(driver)
        val lifetime = StorageLifetime()
        val journal = lifetime.journal(SqlDelightMutationJournalStorage(driver, database))
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        var writer: Deferred<Unit>? = null
        var retirement: Deferred<Unit>? = null
        try {
            val writing = async(Dispatchers.Default) {
                journal.transaction {
                    database.m1Queries.putCollection("held-transaction", "Persisted before close", 0)
                    entered.complete(Unit)
                    check(release.await(10, TimeUnit.SECONDS)) { "Test did not release the transaction" }
                }
            }
            writer = writing
            withTimeout(10_000) { entered.await() }
            val retiring = async(start = CoroutineStart.UNDISPATCHED) { lifetime.retire(); driver.close() }
            retirement = retiring
            assertFalse(retiring.isCompleted)
            assertEquals(0, closeCalls.get(), "The driver cannot close during a leased transaction")
            release.countDown()
            withTimeout(10_000) { writing.await(); retiring.await() }
            assertEquals(1, closeCalls.get())

            val reopened = factory.open("transaction.db")
            try {
                assertEquals("Persisted before close", M1Database(reopened).m1Queries.collections().executeAsList().single().name)
            } finally { reopened.close() }
        } finally {
            release.countDown()
            withContext(NonCancellable) {
                writer?.join()
                retirement?.join()
                if (closeCalls.get() == 0) driver.close()
            }
            directory.deleteRecursively()
        }
    }

    @Test
    fun nestedSourceAndBookkeeperOperationsRemainSynchronousInsideSqlTransaction() = runBlocking {
        val directory = Files.createTempDirectory("trails-storage-nested").toFile()
        val driver = PlatformM1DriverFactory(directory).open("nested.db")
        val database = M1Database(driver)
        val lifetime = StorageLifetime()
        val source = lifetime.source(sqlSource(driver, database))
        val bookkeeper = lifetime.bookkeeper(SqlDelightBookkeeper(driver, database))
        val key = SavedKey("account", "trail")
        val value = SavedValue(key.trailId, setOf("weekend", "favorites"))
        val meta = object : StoreMeta { override val writtenAtEpochMillis = 5L; override val etag: String? = null }
        try {
            source.withTransaction {
                source.write(key, value)
                bookkeeper.recordSuccess(key, meta)
            }
            assertEquals(value, source.reader(key).first())
            assertEquals(5L, bookkeeper.status(key)?.meta?.writtenAtEpochMillis)
            lifetime.retire()
        } finally { driver.close(); directory.deleteRecursively() }
    }

    @Test
    fun everyAdapterEntryRejectsNonCancellableTailWithoutTouchingClosedDriver() = runBlocking {
        val directory = Files.createTempDirectory("trails-storage-retired").toFile()
        val raw = PlatformM1DriverFactory(directory).open("retired.db")
        val calls = AtomicInteger()
        val driver = object : SqlDriver by raw {
            override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
                calls.incrementAndGet()
                return raw.execute(identifier, sql, parameters, binders)
            }
            override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> {
                calls.incrementAndGet()
                return raw.executeQuery(identifier, sql, mapper, parameters, binders)
            }
            override fun newTransaction(): QueryResult<Transacter.Transaction> {
                calls.incrementAndGet()
                return raw.newTransaction()
            }
        }
        val database = M1Database(driver)
        val lifetime = StorageLifetime()
        val source = lifetime.source(sqlSource(driver, database))
        val bookkeeper = lifetime.bookkeeper(SqlDelightBookkeeper(driver, database))
        val journal = lifetime.journal(SqlDelightMutationJournalStorage(driver, database))
        val key = SavedKey("account", "trail")
        val meta = object : StoreMeta { override val writtenAtEpochMillis = 0L; override val etag: String? = null }
        try {
            lifetime.retire()
            raw.close()
            val before = calls.get()
            val operations: List<suspend () -> Unit> = listOf(
                { source.reader(key).first(); Unit },
                { source.write(key, SavedValue(key.trailId, setOf("weekend"))) },
                { source.delete(key) },
                { source.deleteNamespace(key.namespace) },
                { source.deleteAll() },
                { source.withTransaction { error("Retired transaction callback must not run") }; Unit },
                { bookkeeper.recordSuccess(key, meta) },
                { bookkeeper.recordFailure(key, 0) },
                { bookkeeper.status(key); Unit },
                { bookkeeper.forget(key) },
                { bookkeeper.markStale(key) },
                { bookkeeper.advanceStaleWatermark(key.namespace) },
                { bookkeeper.advanceGlobalStaleWatermark() },
                { bookkeeper.forgetNamespace(key.namespace) },
                { bookkeeper.forgetAll() },
                { journal.transaction { error("Retired journal callback must not run") }; Unit },
            )
            withContext(NonCancellable) {
                operations.forEachIndexed { index, operation ->
                    assertFailsWith<CancellationException>("Adapter entry $index must reject retired storage") { operation() }
                }
            }
            assertEquals(before, calls.get(), "No retired wrapper may touch the closed SQL driver")
        } finally {
            raw.close()
            directory.deleteRecursively()
        }
    }
}

private fun sqlSource(driver: SqlDriver, database: M1Database) = SqlDelightSourceOfTruth<SavedKey, SavedValue>(
    driver, database,
    readQuery = { key -> database.m1Queries.readCache(key.namespace.value, key.trailId) { _, _, bytes -> SavedCodec.decode(1, bytes) } },
    writeRow = { key, value -> database.m1Queries.writeCache(key.namespace.value, key.trailId, SavedCodec.encode(value)) },
    deleteRow = { database.m1Queries.deleteCache(it.namespace.value, it.trailId) },
    deleteNamespaceRows = { database.m1Queries.deleteNamespace(it.value) },
    deleteAllRows = { database.m1Queries.deleteAllCache() },
)
