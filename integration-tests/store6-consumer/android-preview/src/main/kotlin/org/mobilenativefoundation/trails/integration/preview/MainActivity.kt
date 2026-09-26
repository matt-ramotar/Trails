package org.mobilenativefoundation.trails.integration.preview

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.mobilenativefoundation.trails.integration.DurableFixtureBackend
import org.mobilenativefoundation.trails.integration.FixtureComposition
import org.mobilenativefoundation.trails.integration.FixtureServices
import org.mobilenativefoundation.trails.integration.FixtureState
import org.mobilenativefoundation.trails.integration.db.FixtureDatabase

class FixtureApplication : Application() {
    lateinit var services: FixtureServices
        private set
    private val accountScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // Separate drivers keep journal operations off the value adapter's gate.
        val values = AndroidSqliteDriver(FixtureDatabase.Schema, this, "alice-values.db")
        val journal = AndroidSqliteDriver(FixtureDatabase.Schema, this, "alice-journal.db")
        val backend = AndroidSqliteDriver(FixtureDatabase.Schema, this, "backend.db")
        services = FixtureServices(
            "alice", values, FixtureDatabase(values), journal, FixtureDatabase(journal),
            DurableFixtureBackend(FixtureDatabase(backend)),
        )
        accountScope.launch { services.initialize() }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val services = (application as FixtureApplication).services
        setContent {
            FixtureComposition(services) { graph ->
                val circuit = remember(graph) {
                    val builder = Circuit.Builder()
                    builder.addUi<FixtureScreen, FixtureState> { state, modifier ->
                        graph.ui.Content(state, modifier)
                    }
                    builder.addPresenter<FixtureScreen, FixtureState>(graph.presenter)
                    builder.build()
                }
                CircuitCompositionLocals(circuit) { CircuitContent(FixtureScreen) }
            }
        }
    }
}

@Parcelize
data object FixtureScreen : Screen
