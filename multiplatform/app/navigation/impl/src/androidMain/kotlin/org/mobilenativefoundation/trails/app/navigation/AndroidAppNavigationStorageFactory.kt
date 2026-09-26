package org.mobilenativefoundation.trails.app.navigation

import android.content.Context

/** An account-partitioned UI snapshot. commit() returns only after the disk write finishes. */
class AndroidAppNavigationStorageFactory(context: Context) : AppNavigationStorageFactory {
    private val appContext = context.applicationContext
    override fun forAccount(accountId: String): AppNavigationStorage {
        val partition = accountId.toByteArray(Charsets.UTF_8).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val preferences = appContext.getSharedPreferences("trails-m1-ui-$partition", Context.MODE_PRIVATE)
        return object : AppNavigationStorage {
            override fun read(): String? = preferences.getString("checkpoint", null)
            override fun write(value: String) {
                val previous = read()
                try {
                    check(preferences.edit().putString("checkpoint", value).commit()) { "Navigation checkpoint could not be written" }
                } catch (failure: Exception) {
                    // SharedPreferences updates its memory map before commit returns. Restore the
                    // last known value too, so another controller cannot read a failed new write.
                    try {
                        val rollback = preferences.edit()
                        if (previous == null) rollback.remove("checkpoint") else rollback.putString("checkpoint", previous)
                        if (!rollback.commit()) failure.addSuppressed(IllegalStateException("Previous navigation checkpoint could not be rewritten"))
                    } catch (rollbackFailure: Exception) { failure.addSuppressed(rollbackFailure) }
                    throw failure
                }
            }
        }
    }
}
