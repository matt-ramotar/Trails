package org.mobilenativefoundation.trails.android.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.mobilenativefoundation.trails.android.App
import org.mobilenativefoundation.trails.android.BuildConfig
import org.mobilenativefoundation.trails.data.trail.account.RealTrailDataFactory
import org.mobilenativefoundation.trails.data.session.model.ActiveUser

/** Debug storage-fault controls. No arbitrary SQL, path, account, reset, or reseed input. */
class StorageFaultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val ordered = isOrderedBroadcast
        val finished = AtomicBoolean(false)
        fun finish(success: Boolean, message: String) {
            if (!finished.compareAndSet(false, true)) return
            try {
                if (ordered) {
                    pending.resultCode = if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED
                    pending.resultData = message
                }
                if (success) Log.i(TAG, message) else Log.e(TAG, message)
            } finally { pending.finish() }
        }
        try {
            check(BuildConfig.DEBUG) { "Fault controls require a debug build" }
            val action = requireNotNull(intent.action) { "An explicit allowlisted action is required" }
            require(action in ACTIONS) { "Unsupported storage fault action" }
            val allowedExtras = if (action == ADOPTION_ON) setOf(TRAIL_ID) else emptySet()
            require(intent.extras?.keySet().orEmpty().all { it in allowedExtras }) { "Unsupported fault-control extras" }
            val app = context.applicationContext as App
            val runtime = app.runtime
            val account = (runtime.userRepository.current as? ActiveUser)?.node?.id
                ?: error("Open Trails and wait for an active signed-in account before injecting faults")
            val factory = runtime.trailData as? RealTrailDataFactory ?: error("The durable trail backend is unavailable")
            val trailId = if (action == ADOPTION_ON) requireNotNull(intent.getStringExtra(TRAIL_ID)) { "trail_id is required" }.also {
                require(it.matches(Regex("[a-z0-9][a-z0-9-]{0,99}"))) { "Invalid trail_id" }
            } else null
            val receiverScope = CoroutineScope(Dispatchers.Main.immediate)
            val job = receiverScope.launch {
                try {
                    val result = withTimeout(7_000) {
                        check((runtime.userRepository.current as? ActiveUser)?.node?.id == account) { "The active account changed" }
                        when (action) {
                            LOSE_ACK -> { factory.loseNextAcknowledgement(); "ARMED next backend acknowledgement loss" }
                            INSPECT -> {
                                val backend = factory.backendEvidence()
                                val local = withContext(Dispatchers.IO) { inspect(context, account) }
                                "INSPECT $local backend=$backend"
                            }
                            else -> withContext(Dispatchers.IO) {
                                changeTrigger(context, account, action, trailId)
                            }
                        }
                    }
                    check((runtime.userRepository.current as? ActiveUser)?.node?.id == account) {
                        "The account changed during the action; inspect controls in the original account $account"
                    }
                    finish(true, "$result account=$account")
                } catch (failure: Throwable) {
                    Log.e(TAG, "Storage fault action failed: $action", failure)
                    finish(false, "FAILED $action: ${failure.message ?: failure::class.simpleName}")
                }
            }
            // Finish the asynchronous broadcast even if cancellation prevents the body from running.
            job.invokeOnCompletion { failure ->
                if (failure != null) finish(false, "FAILED $action: receiver scope stopped")
                receiverScope.cancel()
            }
        } catch (failure: Throwable) {
            Log.e(TAG, "Rejected storage fault action", failure)
            finish(false, "FAILED: ${failure.message ?: failure::class.simpleName}")
        }
    }

    private fun changeTrigger(context: Context, account: String, action: String, trailId: String?): String {
        val admission = action == ADMISSION_ON || action == ADMISSION_OFF
        val enable = action == ADMISSION_ON || action == ADOPTION_ON
        val name = if (admission) ADMISSION_TRIGGER else ADOPTION_TRIGGER
        val suffix = if (admission) "journal" else "values"
        return existingDatabase(context, account, suffix).use { database ->
            database.beginTransaction()
            try {
                if (admission) {
                    database.rawQuery("SELECT account FROM account_identity WHERE id = 1", null).use { rows ->
                        check(rows.moveToFirst() && rows.getString(0) == account) { "Journal account identity does not match" }
                    }
                }
                if (enable) {
                    val condition = if (admission) "NEW.account = ${DatabaseUtils.sqlEscapeString(account)}" else {
                        val selected = requireNotNull(trailId)
                        database.rawQuery("SELECT 1 FROM cache_row WHERE namespace = ? AND canonical_id = ?", arrayOf("saved-$account", selected)).use { rows ->
                            check(rows.moveToFirst()) { "Open the selected trail and its save choices before arming adoption failure; no confirmed base exists" }
                        }
                        "NEW.namespace = ${DatabaseUtils.sqlEscapeString("saved-$account")} AND NEW.canonical_id = ${DatabaseUtils.sqlEscapeString(selected)}"
                    }
                    val table = if (admission) "command_acceptance" else "cache_row"
                    // SQLite stores this text; preserve it so an already armed trigger still matches.
                    val sql = "CREATE TRIGGER $name BEFORE INSERT ON $table WHEN $condition BEGIN SELECT RAISE(ABORT, 'Trails M1 debug ${if (admission) "admission" else "adoption"} failure'); END"
                    val existing = triggerSql(database, name)
                    check(existing == null || existing == sql) { "A different fault scope is already armed; disable it first" }
                    if (existing == null) database.execSQL(sql)
                    check(triggerSql(database, name) == sql) { "Trigger installation could not be verified" }
                } else {
                    database.execSQL("DROP TRIGGER IF EXISTS $name")
                    check(triggerSql(database, name) == null) { "Trigger removal could not be verified" }
                }
                database.setTransactionSuccessful()
            } finally { database.endTransaction() }
            "${if (enable) "ARMED" else "DISARMED"} $name${trailId?.let { " trail=$it" }.orEmpty()}"
        }
    }

    private fun inspect(context: Context, account: String): String {
        val journal = existingDatabase(context, account, "journal").use { database ->
            val receipts = DatabaseUtils.longForQuery(database, "SELECT COUNT(*) FROM command_acceptance WHERE account = ?", arrayOf(account))
            "admissionReceipts=$receipts admissionFault=${triggerSql(database, ADMISSION_TRIGGER) != null}"
        }
        val adoption = existingDatabase(context, account, "values").use { database -> triggerSql(database, ADOPTION_TRIGGER) != null }
        return "$journal adoptionFault=$adoption"
    }

    private fun existingDatabase(context: Context, account: String, suffix: String): SQLiteDatabase {
        check(suffix == "journal" || suffix == "values")
        val partition = account.toByteArray(Charsets.UTF_8).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val file = context.getDatabasePath("trails-m1-$partition-$suffix.db")
        check(file.isFile) { "The active account database is not open yet" }
        val database = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        return try {
            // Bound external SQLite lock waiting below the asynchronous broadcast budget.
            database.rawQuery("PRAGMA busy_timeout = 2000", null).use { result ->
                check(result.moveToFirst() && result.getInt(0) == 2000) { "Could not bound SQLite lock waiting" }
            }
            database
        } catch (failure: Throwable) {
            database.close()
            throw failure
        }
    }

    private fun triggerSql(database: SQLiteDatabase, name: String): String? =
        database.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'trigger' AND name = ?", arrayOf(name)).use { rows ->
            if (rows.moveToFirst()) rows.getString(0) else null
        }

    private companion object {
        const val TAG = "TrailsControlsFault"
        const val PREFIX = "org.mobilenativefoundation.trails.debug."
        const val LOSE_ACK = PREFIX + "LOSE_NEXT_ACK"
        const val ADMISSION_ON = PREFIX + "ADMISSION_FAILURE_ON"
        const val ADMISSION_OFF = PREFIX + "ADMISSION_FAILURE_OFF"
        const val ADOPTION_ON = PREFIX + "ADOPTION_FAILURE_ON"
        const val ADOPTION_OFF = PREFIX + "ADOPTION_FAILURE_OFF"
        const val INSPECT = PREFIX + "INSPECT"
        const val TRAIL_ID = "trail_id"
        const val ADMISSION_TRIGGER = "trails_m1_debug_fail_admission"
        const val ADOPTION_TRIGGER = "trails_m1_debug_fail_adoption"
        val ACTIONS = setOf(LOSE_ACK, ADMISSION_ON, ADMISSION_OFF, ADOPTION_ON, ADOPTION_OFF, INSPECT)
    }
}
