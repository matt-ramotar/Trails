@file:OptIn(org.mobilenativefoundation.store6.core.ExperimentalStoreApi::class)

package org.mobilenativefoundation.trails.data.session

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mobilenativefoundation.store6.core.StoreNamespace
import org.mobilenativefoundation.trails.data.database.TrailsDatabase
import org.mobilenativefoundation.trails.data.database.TrailsDatabaseQueries
import org.mobilenativefoundation.trails.data.session.model.InactiveUser
import org.mobilenativefoundation.trails.data.session.model.LoggedOutUser
import org.mobilenativefoundation.trails.data.session.model.OnboardingStatus
import org.mobilenativefoundation.trails.data.session.model.User
import org.mobilenativefoundation.trails.data.session.model.UserSession

class SessionCompatibilityTest {
    @Test
    fun capturedStore5FilesRetainTheirOriginalHashes() {
        val manifest = Json.parseToJsonElement(resource("manifest.json").decodeToString()).jsonObject
        assertEquals("f096775268af503c509da32fc7f5c21ef127a137", manifest.getValue("producerRevision").jsonPrimitive.content)
        for ((name, hash) in manifest.getValue("files").jsonObject) {
            val actual = MessageDigest.getInstance("SHA-256").digest(resource(name)).joinToString("") {
                (it.toInt() and 255).toString(16).padStart(2, '0')
            }
            assertEquals(hash.jsonPrimitive.content, actual, name)
        }
    }

    @Test
    fun movedModelsReadAndWriteExactlyTheStore5Json() {
        for ((name, expected) in expectedUsers) {
            val encoded = resource("$name.json").decodeToString()
            assertEquals(expected, UserCodec.decode(encoded), name)
            assertEquals(encoded, UserCodec.encode(expected), name)
        }
        val withUnknownField = resource("active.json").decodeToString().dropLast(1) + ",\"futureField\":true}"
        assertEquals(SampleAccounts.primary, UserCodec.decode(withUnknownField))
    }

    @Test
    fun freshSchemaMatchesTheCapturedVersionTwoDatabase() {
        SessionFixture("empty").use { fixture ->
            val fresh = fixture.path.resolveSibling("fresh.db")
            val driver = JdbcSqliteDriver("jdbc:sqlite:$fresh")
            try {
                TrailsDatabase.Schema.create(driver)
                assertEquals(tableDefinitions(fixture.path), tableDefinitions(fresh))
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun firstEmissionRestoresOldRowsWithoutAnIntermediateLoggedOutUser(): Unit = runBlocking {
        for ((name, expected) in expectedUsers + ("empty" to LoggedOutUser)) {
            SessionFixture(name).use { fixture ->
                assertSame(LoggedOutUser, fixture.repository.current)
                assertEquals(expected, withTimeout(5_000) { fixture.repository.stream().first() }, name)
                assertEquals(expected, fixture.repository.current)
            }
        }
    }

    @Test
    fun missingRowRemainsObservableThroughSignInSwitchAndSignOut(): Unit = runBlocking {
        SessionFixture("empty").use { fixture ->
            fixture.repository.stream().distinctUntilChanged().test {
                assertSame(LoggedOutUser, awaitItem())
                fixture.repository.persist(SampleAccounts.primary)
                assertEquals(SampleAccounts.primary, awaitItem())
                fixture.repository.persist(SampleAccounts.secondary)
                assertEquals(SampleAccounts.secondary, awaitItem())
                fixture.repository.persist(LoggedOutUser)
                assertSame(LoggedOutUser, awaitItem())
                assertEquals("{}", fixture.queries.userStateQueries.selectById("current").executeAsOne())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun committedSessionSurvivesAClosedAndReopenedDatabase(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            fixture.repository.persist(SampleAccounts.secondary)
            fixture.closeRuntime()
            fixture.reopen()
            assertEquals(SampleAccounts.secondary, withTimeout(5_000) { fixture.repository.stream().first() })
            fixture.repository.persist(LoggedOutUser)
            fixture.closeRuntime()
            fixture.reopen()
            assertSame(LoggedOutUser, withTimeout(5_000) { fixture.repository.stream().first() })
            assertEquals(originalTables, fixture.tableNames())
            DriverManager.getConnection("jdbc:sqlite:${fixture.path}").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version").use { rows ->
                        assertTrue(rows.next())
                        assertEquals(TrailsDatabase.Schema.version, rows.getLong(1))
                    }
                }
            }
        }
    }

    @Test
    fun currentTracksObservedValuesRatherThanUnobservedWrites(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            assertEquals(SampleAccounts.primary, withTimeout(5_000) { fixture.repository.stream().first() })
            fixture.repository.persist(SampleAccounts.secondary)
            assertEquals(SampleAccounts.primary, fixture.repository.current)
            assertEquals(SampleAccounts.secondary, withTimeout(5_000) { fixture.repository.stream().first() })
            assertEquals(SampleAccounts.secondary, fixture.repository.current)
        }
    }

    @Test
    fun corruptSessionReachesTheRecoveryCallerInsteadOfSigningOut(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            fixture.queries.userStateQueries.upsert("current", "{invalid-json")
            assertFailsWith<kotlinx.serialization.SerializationException> {
                withTimeout(5_000) { fixture.repository.stream().first() }
            }
        }
    }

    @Test
    fun readFailureRetainsItsCauseAndCanBeRetried(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            val failure = IllegalStateException("injected user_state read failure")
            fixture.driver.readFailure = failure
            assertOriginalCause(failure, assertFailsWith<IllegalStateException> {
                withTimeout(5_000) { fixture.repository.stream().first() }
            })
            fixture.driver.readFailure = null
            assertEquals(SampleAccounts.primary, withTimeout(5_000) { fixture.repository.stream().first() })
        }
    }

    @Test
    fun failedWriteDoesNotPublishOrReplaceTheCommittedSession(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            fixture.repository.stream().distinctUntilChanged().test {
                assertEquals(SampleAccounts.primary, awaitItem())
                val failure = IllegalStateException("injected user_state write failure")
                fixture.driver.writeFailure = failure
                assertOriginalCause(failure, assertFailsWith<IllegalStateException> {
                    fixture.repository.persist(SampleAccounts.secondary)
                })
                fixture.driver.writeFailure = null
                assertEquals(SampleAccounts.primary, UserCodec.decode(fixture.queries.userStateQueries.selectById("current").executeAsOne()))
                assertEquals(SampleAccounts.primary, fixture.repository.current)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun sourceReadersStayLiveAcrossEqualWritesAndEveryDeleteScope(): Unit = runBlocking {
        SessionFixture("empty").use { fixture ->
            val source = fixture.source()
            source.reader(CurrentUserKey).test {
                assertNull(awaitItem())
                source.write(CurrentUserKey, SampleAccounts.primary)
                assertEquals(SampleAccounts.primary, awaitItem())
                source.write(CurrentUserKey, SampleAccounts.primary)
                assertEquals(SampleAccounts.primary, awaitItem())
                source.deleteNamespace(StoreNamespace("unrelated"))
                expectNoEvents()
                source.delete(CurrentUserKey)
                assertNull(awaitItem())
                source.write(CurrentUserKey, SampleAccounts.secondary)
                assertEquals(SampleAccounts.secondary, awaitItem())
                source.deleteNamespace(StoreNamespace("session"))
                assertNull(awaitItem())
                source.write(CurrentUserKey, SampleAccounts.primary)
                assertEquals(SampleAccounts.primary, awaitItem())
                source.deleteAll()
                assertNull(awaitItem())
                assertNull(source.reader(CurrentUserKey).first())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun failureAfterSqlExecutionRollsBackBeforeAnyReaderNotification(): Unit = runBlocking {
        for (failure in listOf(IllegalStateException("after SQL"), CancellationException("after SQL"))) {
            SessionFixture("active").use { fixture ->
                val source = fixture.source()
                source.reader(CurrentUserKey).test {
                    assertEquals(SampleAccounts.primary, awaitItem())
                    fixture.driver.afterWrite = { throw failure }
                    try {
                        source.write(CurrentUserKey, SampleAccounts.secondary)
                        error("write should fail")
                    } catch (actual: Throwable) {
                        assertOriginalCause(failure, actual)
                    }
                    fixture.driver.afterWrite = null
                    assertEquals(SampleAccounts.primary, source.reader(CurrentUserKey).first())
                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    @Test
    fun admittedWriteCommitsDespiteCancellationWhileQueuedWriteStaysUnapplied(): Unit = runBlocking {
        SessionFixture("empty").use { fixture ->
            val source = fixture.source()
            val enteredSql = CountDownLatch(1)
            val releaseSql = CountDownLatch(1)
            val returned = CompletableDeferred<Throwable?>()
            fixture.driver.afterWrite = {
                enteredSql.countDown()
                check(releaseSql.await(5, TimeUnit.SECONDS))
            }
            val admitted = launch(Dispatchers.Default) {
                try {
                    source.write(CurrentUserKey, SampleAccounts.primary)
                    returned.complete(null)
                } catch (failure: Throwable) {
                    returned.complete(failure)
                }
            }
            try {
                assertTrue(enteredSql.await(5, TimeUnit.SECONDS))
                val queued = launch(start = CoroutineStart.UNDISPATCHED) {
                    source.write(CurrentUserKey, SampleAccounts.secondary)
                    error("cancelled queued write must not return")
                }
                queued.cancelAndJoin()
                admitted.cancel()
            } finally {
                releaseSql.countDown()
            }
            admitted.join()
            assertNull(returned.await())
            fixture.driver.afterWrite = null
            assertEquals(SampleAccounts.primary, source.reader(CurrentUserKey).first())
        }
    }

    @Test
    fun appScopeCompletionClosesTheStore(): Unit = runBlocking {
        SessionFixture("active").use { fixture ->
            fixture.scope.cancel()
            assertFailsWith<IllegalStateException> { fixture.repository.persist(SampleAccounts.secondary) }
            assertFailsWith<IllegalStateException> { fixture.repository.stream().first() }
            assertFalse(fixture.driver.closed)
        }
    }

    private companion object {
        val expectedUsers: Map<String, User> = linkedMapOf(
            "active" to SampleAccounts.primary,
            "inactive" to InactiveUser.Composite(
                InactiveUser.Node("cleanup-inactive-account", InactiveUser.Properties(
                    UserSession("cleanup-inactive-session", "local-cleanup-inactive-token"),
                    OnboardingStatus.Incomplete,
                    null,
                )),
                InactiveUser.Edges,
            ),
            "logged-out" to LoggedOutUser,
        )
        val originalTables = setOf("Chat", "developer_settings", "feed_posts", "posts", "user_state")
    }
}

private fun resource(name: String): ByteArray = checkNotNull(
    SessionCompatibilityTest::class.java.getResourceAsStream("/session-upgrade/$name"),
) { "Missing captured session fixture: $name" }.use { it.readAllBytes() }

private fun assertOriginalCause(expected: Throwable, actual: Throwable) {
    assertEquals(expected::class, actual::class)
    assertEquals(expected.message, actual.message)
    // Coroutine stack recovery may copy the exception and retain the original as its cause.
    assertTrue(generateSequence(actual) { it.cause }.any { it === expected })
}

private fun tableDefinitions(path: Path): Map<String, String> = DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
    connection.createStatement().use { statement ->
        statement.executeQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table'").use { rows ->
            buildMap { while (rows.next()) put(rows.getString(1), rows.getString(2)) }
        }
    }
}

private class SessionFixture(name: String) : AutoCloseable {
    val path: Path = Files.createTempDirectory("trails-session-test-").resolve("trails.db")
    lateinit var driver: SessionFaultDriver
    lateinit var queries: TrailsDatabaseQueries
    lateinit var scope: CoroutineScope
    lateinit var repository: RealUserRepository

    init {
        Files.write(path, resource("$name.db"))
        reopen()
    }

    fun reopen() {
        driver = SessionFaultDriver(JdbcSqliteDriver("jdbc:sqlite:$path"))
        queries = TrailsDatabaseQueries(driver)
        scope = CoroutineScope(SupervisorJob())
        repository = RealUserRepository(queries, scope, Dispatchers.Unconfined)
    }

    fun source() = UserSourceOfTruth(queries.userStateQueries, Dispatchers.IO)

    fun tableNames(): Set<String> = DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rows ->
                buildSet { while (rows.next()) add(rows.getString(1)) }
            }
        }
    }

    fun closeRuntime() {
        scope.cancel()
        driver.close()
    }

    override fun close() {
        closeRuntime()
        path.parent.toFile().deleteRecursively()
    }
}

private class SessionFaultDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    @Volatile var readFailure: Throwable? = null
    @Volatile var writeFailure: Throwable? = null
    @Volatile var afterWrite: (() -> Unit)? = null
    var closed = false
        private set

    override fun <R> executeQuery(
        identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        if (sql.contains("FROM user_state", ignoreCase = true)) readFailure?.let { throw it }
        return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    override fun execute(
        identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
        val sessionWrite = sql.contains("INTO user_state", ignoreCase = true) || sql.contains("DELETE FROM user_state", ignoreCase = true)
        if (sessionWrite) writeFailure?.let { throw it }
        val result = delegate.execute(identifier, sql, parameters, binders)
        if (sessionWrite) afterWrite?.invoke()
        return result
    }

    override fun close() {
        if (!closed) {
            closed = true
            delegate.close()
        }
    }
}
