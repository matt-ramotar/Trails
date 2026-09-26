package org.mobilenativefoundation.trails.data.developersettings

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.trails.data.database.TrailsDatabase
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.foundation.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperSettingsCompatibilityTest {
    @Test
    fun settingsReadFailureCanRetryWithoutEmittingSyntheticOnlineSettings() = runTest {
        Fixture(this).use { fixture ->
            fixture.queries.developerSettingsQueries.upsertSettings(
                1L, 0L, "SERVER_WINS", 50L, 200L, 0.0, 0L, "DISABLED", 0.0, "RANDOM",
            )
            fixture.driver.readFailure = IllegalStateException("Disk unavailable")
            assertEquals("Saved runtime settings could not be restored.", fixture.repository.readFailures().first { it != null })
            fixture.driver.readFailure = null
            fixture.repository.retryRead()
            assertEquals(true, fixture.repository.stream().first().offlineMode)
        }
    }

    @Test
    fun firstEmissionRestoresOfflineBeforeAnyBackendCanFetch() = runTest {
        Fixture(this).use { fixture ->
            fixture.queries.developerSettingsQueries.upsertSettings(
                1L, 0L, "SERVER_WINS", 50L, 200L, 0.0, 0L, "DISABLED", 0.0, "RANDOM",
            )
            // No runCurrent: a synthetic initial Online state would allow an early fetch.
            assertEquals(true, fixture.repository.stream().first().offlineMode)
        }
    }

    @Test
    fun updatesAndResetPreserveUnusedStoredStrategy() = runTest {
        Fixture(this).use { fixture ->
            for (stored in listOf("SERVER_WINS", "CLIENT_WINS", "MERGE", "LAST_WRITE_WINS", "future-strategy")) {
                fixture.persistRaw(strategy = stored)
                runCurrent()
                assertEquals(DeveloperSettings(), fixture.repository.current)

                fixture.repository.setOfflineMode(true)
                runCurrent()
                assertEquals(true, fixture.repository.current.offlineMode)
                assertEquals(stored, fixture.queries.developerSettingsQueries.selectSettings().executeAsOne().conflictStrategy)

                fixture.repository.resetToDefaults()
                runCurrent()
                assertEquals(DeveloperSettings(), fixture.repository.current)
                assertEquals(stored, fixture.queries.developerSettingsQueries.selectSettings().executeAsOne().conflictStrategy)
            }
        }
    }

    @Test
    fun allLegacyAndCurrentConflictModesDecode() = runTest {
        Fixture(this).use { fixture ->
            val expected = BackendConflictMode.entries.associateBy { it.name } + mapOf(
                "FIRST_WRITE" to BackendConflictMode.HTTP_409,
                "EVERY_WRITE" to BackendConflictMode.HTTP_409,
                "RANDOM" to BackendConflictMode.HTTP_409,
                "MANUAL" to BackendConflictMode.HTTP_409,
                "future-mode" to BackendConflictMode.DISABLED,
            )
            for ((stored, mode) in expected) {
                fixture.persistRaw(mode = stored)
                runCurrent()
                assertEquals(mode, fixture.repository.current.backendConflictMode, stored)
            }
        }
    }

    @Test
    fun allLegacyAndCurrentSeedPresetsDecode() = runTest {
        Fixture(this).use { fixture ->
            val expected = BackendSimulationSeedPreset.entries.associateBy { it.name } +
                listOf("DEFAULT", "EMPTY", "LARGE_DATASET", "CONFLICT_TESTING", "ERROR_STATES", "future-seed")
                    .associateWith { BackendSimulationSeedPreset.RANDOM }
            for ((stored, preset) in expected) {
                fixture.persistRaw(seed = stored)
                runCurrent()
                assertEquals(preset, fixture.repository.current.simulationSeedPreset, stored)
            }
        }
    }

    @Test
    fun emptyDatabaseAndResetUseCompleteDefaults() = runTest {
        Fixture(this).use { fixture ->
            runCurrent()
            assertEquals(DeveloperSettings(), fixture.repository.current)
            val custom = DeveloperSettings(
                offlineMode = true,
                conflictsEnabled = true,
                latencyMinMs = 10,
                latencyMaxMs = 400,
                errorRate = 0.25f,
                rateLimitPerMinute = 50,
                backendConflictMode = BackendConflictMode.HTTP_409,
                conflictProbability = 0.75f,
                simulationSeedPreset = BackendSimulationSeedPreset.SEED_1337,
            )
            fixture.repository.update(custom)
            runCurrent()
            assertEquals(custom, fixture.repository.current)
            assertEquals("SERVER_WINS", fixture.queries.developerSettingsQueries.selectSettings().executeAsOne().conflictStrategy)

            fixture.repository.resetToDefaults()
            runCurrent()

            assertEquals(DeveloperSettings(), fixture.repository.current)
            val reloaded = RealDeveloperSettingsRepository(
                backgroundScope, fixture.queries, Logger(), StandardTestDispatcher(testScheduler),
            )
            runCurrent()
            assertEquals(DeveloperSettings(), reloaded.current)
        }
    }

    @Test
    fun failedUpdateIsReportedAndPreservesPersistedSettings() = runTest {
        Fixture(this).use { fixture ->
            fixture.repository.update(DeveloperSettings(offlineMode = true))
            runCurrent()
            val failure = IllegalStateException("Disk write failed")
            fixture.driver.writeFailure = failure

            val reported = assertFailsWith<IllegalStateException> {
                fixture.repository.setOfflineMode(false)
            }
            assertEquals(failure.message, reported.message)
            runCurrent()

            assertEquals(true, fixture.repository.current.offlineMode)
            assertEquals(1L, fixture.queries.developerSettingsQueries.selectSettings().executeAsOne().offlineMode)
        }
    }

    @Test
    fun failedResetIsReportedAndKeepsPriorValues() = runTest {
        Fixture(this).use { fixture ->
            val custom = DeveloperSettings(offlineMode = true, simulationSeedPreset = BackendSimulationSeedPreset.SEED_42)
            fixture.repository.update(custom)
            runCurrent()
            fixture.driver.writeFailure = IllegalStateException("Disk write failed")

            assertFailsWith<IllegalStateException> { fixture.repository.resetToDefaults() }
            runCurrent()

            assertEquals(custom, fixture.repository.current)
            assertEquals("SEED_42", fixture.queries.developerSettingsQueries.selectSettings().executeAsOne().simulationSeedPreset)
        }
    }

    @Test
    fun cancellationFromPersistencePropagatesAsCancellation() = runTest {
        Fixture(this).use { fixture ->
            runCurrent()
            val cancellation = CancellationException("Settings write cancelled")
            fixture.driver.writeFailure = cancellation

            val reported = assertFailsWith<CancellationException> {
                fixture.repository.setOfflineMode(true)
            }
            assertEquals(cancellation.message, reported.message)
            assertEquals(DeveloperSettings(), fixture.repository.current)
        }
    }

    private class Fixture(scope: TestScope) : AutoCloseable {
        private val sqlite = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TrailsDatabase.Schema.create(it) }
        val driver = FailingWriteDriver(sqlite)
        val queries = TrailsDatabaseQueries(driver)
        val repository = RealDeveloperSettingsRepository(
            scope.backgroundScope, queries, Logger(), StandardTestDispatcher(scope.testScheduler),
        )

        fun persistRaw(strategy: String = "SERVER_WINS", mode: String = "DISABLED", seed: String = "RANDOM") {
            queries.developerSettingsQueries.upsertSettings(0L, 0L, strategy, 50L, 200L, 0.0, 0L, mode, 0.0, seed)
        }

        override fun close() = sqlite.close()
    }

    private class FailingWriteDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
        var writeFailure: Throwable? = null
        var readFailure: Throwable? = null

        override fun <R> executeQuery(
            identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>,
            parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<R> {
            if (sql.contains("FROM developer_settings")) readFailure?.let { throw it }
            return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
        }

        override fun execute(
            identifier: Int?,
            sql: String,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<Long> {
            if (sql.contains("INSERT OR REPLACE INTO developer_settings")) {
                writeFailure?.let { throw it }
            }
            return delegate.execute(identifier, sql, parameters, binders)
        }
    }
}
