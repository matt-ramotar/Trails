# Trails

Kotlin Multiplatform app targeting Android, iOS, JVM, and Web using Compose Multiplatform.

## Build Commands

```bash
# Full build
./gradlew build

# Per-module builds (faster feedback)
./gradlew :multiplatform:screen:profile:impl:compileKotlinJvm
./gradlew :multiplatform:foundation:designsystem:compileKotlinJvm

# Android-specific
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:installDebug
```

## Validation

Always validate changes by running an appropriate Gradle build/compile task before responding. Prefer the smallest relevant module task for quick feedback, and run a full build when requested.

## Project Structure

```
Trails/
├── apps/
│   └── android/            # Android entry point
├── multiplatform/
│   ├── app/                # App bootstrap & scaffold (api/impl)
│   ├── atom/               # Atom-based state machines
│   ├── data/{name}/        # Data layer (api + impl)
│   ├── di/
│   │   ├── scope/          # DI scopes
│   │   └── graph/          # DI graphs (app, active, inactive, loggedin, loggedout)
│   ├── feat/{name}/        # Cross-cutting features (api + impl)
│   ├── foundation/         # designsystem, networking, logging, coroutines, parcel
│   ├── lib/                # Additional libraries
│   ├── model/              # network, domain, db models
│   └── screen/{name}/      # Navigation destinations (api + impl)
├── server/                 # Server-side code
└── tooling/plugins/        # Gradle convention plugins
```

## User State Model

User state drives DI graph lifecycle:

```kotlin
sealed interface User
object LoggedOutUser : User
sealed interface LoggedInUser : User {
    sealed interface InactiveUser : LoggedInUser  // Onboarding incomplete
    sealed interface ActiveUser : LoggedInUser    // Fully onboarded
}
```

## Bootstrap Coordinator

The `BootstrapCoordinator` manages app initialization:
1. Watches `UserRepository.stream()` for state changes
2. Creates/destroys DI graphs based on user state
3. Routes users through: Splash → Welcome → PreLanding → Main

| User State | Graph Created | Initial Screen |
|------------|---------------|----------------|
| `LoggedOutUser` | `LoggedOutGraph` | `WelcomeScreen` |
| `InactiveUser` | `LoggedInGraph` + `InactiveGraph` | `PreLandingScreen` |
| `ActiveUser` | `LoggedInGraph` + `ActiveGraph` | `HomeScreen` |

## Screen vs Feature

| Directory | Purpose | Examples |
|-----------|---------|----------|
| `screen/` | Navigation destinations - full-screen UI registered with Circuit | `home`, `profile`, `settings`, `login` |
| `feat/` | Cross-cutting features - reusable functionality invoked from screens | `paywall`, `share`, `onboarding`, `rating` |

**Key distinction**: Screens are where users navigate to. Features are what screens can trigger.

```
Example: Paywall feature
├── feat/paywall/           # The paywall feature module
│   ├── api/                # PaywallTrigger interface, PaywallResult
│   └── impl/               # RealPaywallTrigger, PaywallModal
└── screen/home/            # Home screen invokes paywall
    └── impl/               # HomePresenter calls PaywallTrigger
```

## Screen Development

### Required Module Structure

```
multiplatform/screen/{feature}/
├── api/
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/{feature}/
│       ├── {Feature}Screen.kt      # Circuit Screen marker
│       ├── {Feature}State.kt       # UI state
│       └── {Feature}Intent.kt      # User actions
└── impl/
    ├── build.gradle.kts
    └── src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/{feature}/
        ├── {Feature}Presenter.kt   # State logic
        └── {Feature}Ui.kt          # Compose UI
```

### API Module (build.gradle.kts)

```kotlin
plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(projects.multiplatform.model.domain)
                implementation(projects.multiplatform.foundation.designsystem)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.{feature}.api"
}
```

### IMPL Module (build.gradle.kts)

```kotlin
plugins {
    id("plugin.trails.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.screen.{feature}.api)
                implementation(projects.multiplatform.foundation.designsystem)
            }
        }
    }
}

android {
    namespace = "org.mobilenativefoundation.trails.screen.{feature}.impl"
}
```

### Screen Definition (API)

```kotlin
package org.mobilenativefoundation.trails.screen.{feature}

import com.slack.circuit.runtime.screen.Screen
import org.mobilenativefoundation.trails.foundation.parcel.Parcelize

@Parcelize
data class {Feature}Screen(
    val param: String? = null
) : Screen
```

### State & Intent (API)

```kotlin
// {Feature}State.kt
import com.slack.circuit.runtime.CircuitUiState

sealed interface {Feature}State : CircuitUiState {
    data object Initial : {Feature}State
    data class Data(
        val items: List<Item>,
        val isLoading: Boolean = false,
        val send: ({Feature}Intent) -> Unit
    ) : {Feature}State
}

// {Feature}Intent.kt
import com.slack.circuit.runtime.CircuitUiEvent

sealed interface {Feature}Intent : CircuitUiEvent {
    data object Refresh : {Feature}Intent
    data class SelectItem(val id: String) : {Feature}Intent
}
```

### Presenter (IMPL)

```kotlin
package org.mobilenativefoundation.trails.screen.{feature}

import androidx.compose.runtime.*
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class {Feature}Presenter() : Presenter<{Feature}State> {
    @Composable
    override fun present(): {Feature}State {
        var data by remember { mutableStateOf(initialData) }

        val send = { intent: {Feature}Intent ->
            when (intent) {
                is {Feature}Intent.Refresh -> { /* handle */ }
                is {Feature}Intent.SelectItem -> { /* handle */ }
            }
        }

        return {Feature}State.Data(items = data, send = send)
    }
}
```

### UI Component (IMPL)

```kotlin
package org.mobilenativefoundation.trails.screen.{feature}

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class {Feature}Ui : Ui<{Feature}State> {
    @Composable
    override fun Content(state: {Feature}State, modifier: Modifier) {
        // Compose UI here
    }
}
```

### DI Registration (ActiveGraph.kt)

After creating a screen, register it in `multiplatform/di/graph/active/.../ActiveGraph.kt`:

```kotlin
// 1. Add imports
import org.mobilenativefoundation.trails.screen.{feature}.{Feature}Presenter
import org.mobilenativefoundation.trails.screen.{feature}.{Feature}Screen
import org.mobilenativefoundation.trails.screen.{feature}.{Feature}State
import org.mobilenativefoundation.trails.screen.{feature}.{Feature}Ui

// 2. Add to provideCircuit() parameters
fun provideCircuit(
    // ... existing params
    {feature}Ui: {Feature}Ui,
    {feature}Presenter: {Feature}Presenter,
): Circuit {
    val builder = Circuit.Builder()

    // 3. Register UI and Presenter
    builder.addUi<{Feature}Screen, {Feature}State> { state, modifier ->
        {feature}Ui.Content(state, modifier)
    }
    builder.addPresenter<{Feature}Screen, {Feature}State> { _, _, _ ->
        {feature}Presenter
    }

    return builder.build()
}
```

## Feature Development

Features are cross-cutting functionality that can be triggered from multiple screens. Unlike screens, features don't register with Circuit - they're injected into screens that need them.

### Required Module Structure

```
multiplatform/feat/{feature}/
├── api/
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/{feature}/
│       ├── {Feature}Trigger.kt     # Interface to invoke the feature
│       └── {Feature}Result.kt      # Result type (if needed)
└── impl/
    ├── build.gradle.kts
    └── src/commonMain/kotlin/org/mobilenativefoundation/trails/feat/{feature}/
        ├── Real{Feature}Trigger.kt # Implementation
        └── {Feature}Modal.kt       # UI component (if modal-based)
```

### Feature API (api/)

```kotlin
package org.mobilenativefoundation.trails.feat.{feature}

// Trigger interface - how screens invoke the feature
interface {Feature}Trigger {
    suspend fun show(): {Feature}Result
}

// Result type
sealed interface {Feature}Result {
    data object Success : {Feature}Result
    data object Dismissed : {Feature}Result
    data class Error(val message: String) : {Feature}Result
}
```

### Feature Implementation (impl/)

```kotlin
package org.mobilenativefoundation.trails.feat.{feature}

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@ContributesBinding(ActiveScope::class)
@Inject
class Real{Feature}Trigger(
    // Dependencies like Navigator, Analytics, etc.
) : {Feature}Trigger {
    override suspend fun show(): {Feature}Result {
        // Show modal, navigate, or perform action
        return {Feature}Result.Success
    }
}
```

### Using Features in Screens

```kotlin
// In screen's Presenter - inject the feature trigger
@ContributesBinding(ActiveScope::class)
@Inject
class HomePresenter(
    private val paywallTrigger: PaywallTrigger,  // Injected feature
) : Presenter<HomeState> {
    @Composable
    override fun present(): HomeState {
        val coroutineScope = rememberCoroutineScope()

        val send = { intent: HomeIntent ->
            when (intent) {
                is HomeIntent.UpgradeTapped -> {
                    coroutineScope.launch {
                        val result = paywallTrigger.show()
                        // Handle result
                    }
                }
            }
        }
        // ...
    }
}
```

### Feature vs Screen Decision Guide

| Use `screen/` when... | Use `feat/` when... |
|-----------------------|---------------------|
| User navigates TO it | Screen INVOKES it |
| Has its own URL/route | No dedicated route |
| Full-screen destination | Modal, bottom sheet, or overlay |
| Single entry point | Multiple entry points |
| Example: `ProfileScreen` | Example: `PaywallTrigger` |

## Data Layer

### API Module

```kotlin
// UserRepository.kt
interface UserRepository : UserStateReader, UserStateWriter

// UserStateReader.kt
interface UserStateReader {
    fun stream(): Flow<User>
    val current: User
}

// UserStateWriter.kt
interface UserStateWriter {
    fun updateState(user: User)
}
```

### IMPL Module

```kotlin
@ContributesBinding(AppScope::class)
@Inject
class RealUserRepository(
    @param:Named("AppCoroutineScope") private val appScope: CoroutineScope,
    private val storage: UserStateStorage,
    private val logger: Logger,
) : UserRepository {
    private val _state = MutableStateFlow<User>(LoggedOutUser)

    override fun stream(): Flow<User> = _state.asStateFlow()
    override val current: User get() = _state.value
    override fun updateState(user: User) { _state.value = user }
}
```

## Atom (Complex State Management)

Use Atom for screens with complex state machines, multi-step flows, or side effects. For simple screens, Presenters suffice.

### When to Use Atom

- Multi-step wizards or flows
- Real-time synchronization
- Complex form validation
- Screens with many side effects (API calls, persistence)
- State that survives process death

### Atom Module Structure

```
multiplatform/atom/
└── src/commonMain/kotlin/org/mobilenativefoundation/trails/atom/{feature}/
    └── {Feature}Atom.kt
```

### Atom build.gradle.kts

```kotlin
plugins {
    id("plugin.trails.feature")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.atom)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.multiplatform.di.scope)
                api(libs.atom.runtime)
                api(libs.atom.metro)
                // Reference screen API for State/Intent/Event types
                api(projects.multiplatform.screen.{feature}.api)
            }
        }
    }
}

atom {
    di = DI.METRO      // Use Metro for DI
    compose = true     // Enable Compose integration
    strict = true      // Strict mode for better errors
}
```

### Atom Definition

```kotlin
package org.mobilenativefoundation.trails.atom.{feature}

import dev.mattramotar.atom.runtime.Atom
import dev.mattramotar.atom.runtime.annotations.AutoAtom
import dev.mattramotar.atom.runtime.annotations.InitialState
import dev.mattramotar.atom.runtime.fsm.Transition
import dev.mattramotar.atom.runtime.state.StateHandle
import kotlinx.coroutines.CoroutineScope
import org.mobilenativefoundation.trails.foundation.scope.ActiveScope

@AutoAtom(ActiveScope::class)
class {Feature}Atom(
    scope: CoroutineScope,
    handle: StateHandle<{Feature}State>
) : Atom<{Feature}State, {Feature}Intent, {Feature}Event, {Feature}Effect>(scope, handle) {

    companion object {
        @InitialState
        fun initial(params: {Feature}Params): {Feature}State = {Feature}State(id = params.id)
    }

    override fun reduce(state: {Feature}State, event: {Feature}Event): Transition<{Feature}State, {Feature}Effect> {
        return when (event) {
            {Feature}Event.DataLoaded -> Transition(
                to = state.copy(isLoading = false),
                effects = emptyList()
            )
            {Feature}Event.Error -> Transition(
                to = state.copy(error = true),
                effects = listOf({Feature}Effect.ShowError)
            )
        }
    }

    override fun intent(intent: {Feature}Intent) {
        when (intent) {
            {Feature}Intent.Refresh -> dispatch({Feature}Event.Loading)
            is {Feature}Intent.Submit -> {
                // Dispatch events based on intent
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Initialize: start collecting side effects, load initial data
    }

    override fun onStop() {
        super.onStop()
        // Cleanup: cancel any ongoing operations
    }
}
```

### Atom API Types (in screen/{feature}/api/)

```kotlin
// {Feature}State.kt - must be @Serializable for persistence
@Serializable
data class {Feature}State(
    val id: String,
    val isLoading: Boolean = true,
    val error: Boolean = false
)

// {Feature}Intent.kt - user actions
sealed interface {Feature}Intent {
    data object Refresh : {Feature}Intent
    data class Submit(val data: String) : {Feature}Intent
}

// {Feature}Event.kt - internal state machine events
sealed interface {Feature}Event {
    data object Loading : {Feature}Event
    data object DataLoaded : {Feature}Event
    data object Error : {Feature}Event
}

// {Feature}Effect.kt - side effects to execute
sealed interface {Feature}Effect {
    data object ShowError : {Feature}Effect
    data object Navigate : {Feature}Effect
}

// {Feature}Params.kt - initialization parameters
@Parcelize
data class {Feature}Params(val id: String) : Parcelable
```

### Using Atom in Compose

```kotlin
@Composable
fun {Feature}Content() {
    val atom = atom<{Feature}Atom>(params = {Feature}Params(id = "123"))
    val state by atom.state.collectAsState()

    Button(onClick = { atom.intent({Feature}Intent.Refresh) }) {
        Text("Refresh")
    }
}
```

### Key Atom Concepts

| Concept | Purpose |
|---------|---------|
| `Intent` | User action that enters the system |
| `Event` | Internal event that triggers state change |
| `reduce()` | Pure function: (State, Event) → Transition |
| `Transition` | New state + list of Effects |
| `Effect` | Side effect (API call, navigation, analytics) |
| `StateHandle` | Manages state persistence |
| `@InitialState` | Factory method for initial state |
| `@AutoAtom` | KSP annotation for code generation |

## Foundation Modules

| Module | Purpose | Key Types |
|--------|---------|-----------|
| `designsystem` | Compose UI components, theming | `TrailsTheme`, `Colors`, `Typography`, components |
| `networking` | HTTP client via Ktor | `HttpClientProvider`, `AuthTokenHolder`, `NetworkConfig` |
| `logging` | Logging facade | `Logger` interface |
| `parcel` | Multiplatform parceling | `@Parcelize`, `Parcelable` |
| `coroutines` | Dispatcher qualifiers | `@Io` dispatcher |

## TrailsTheme Usage

**Always use `TrailsTheme` instead of `MaterialTheme`** for consistent theming across the app.

### Accessing Theme Tokens

```kotlin
@Composable
fun MyComponent() {
    // Extract theme tokens at the top of composables
    val typography = TrailsTheme.typography
    val colors = TrailsTheme.colors
    val spacing = TrailsTheme.spacing
    val radii = TrailsTheme.radii
    val gradients = TrailsTheme.gradients

    // Use local variables throughout
    Text(
        text = "Hello",
        style = typography.bodyMedium,
        color = colors.textPrimary
    )
}
```

### Available Tokens

| Token | Access | Examples |
|-------|--------|----------|
| Typography | `TrailsTheme.typography` | Material3: `bodyMedium`, `titleLarge`, `labelSmall` |
| Text Styles | `TrailsTheme.textStyles` | Custom: `message`, `timestamp`, `codeInline`, `button` |
| Colors | `TrailsTheme.colors` | Extended colors (see below) |
| Color Scheme | `TrailsTheme.colorScheme` | Material3: `primary`, `secondary`, `surface` |
| Spacing | `TrailsTheme.spacing` | `xs` (4dp), `sm` (8dp), `md` (12dp), `lg` (16dp), `xl` (24dp), `xxl` (32dp) |
| Radii | `TrailsTheme.radii` | `sm` (8dp), `md` (12dp), `lg` (16dp), `xl` (24dp), `pill` (999dp) |
| Gradients | `TrailsTheme.gradients` | `sunset`, `ocean`, `mint`, `rose`, `brand`, `location`, `friends`, `quickSnap`, `activeTabIndicator`, `deepPurple` |

### TrailsExtendedColors (Domain-Specific)

Access via `TrailsTheme.colors`:

| Category | Tokens | Purpose |
|----------|--------|---------|
| **Difficulty Ratings** | `greenCircle`, `blueSquare`, `blackDiamond`, `doubleBlack` | Ski trail difficulty indicators |
| **Trail Conditions** | `freshTrails`, `packedTrails`, `icy`, `groomed` | Current snow/trail status |
| **Lift Status** | `liftOpen`, `liftClosed` | Lift operational state |
| **Crew Status** | `crewOnline`, `crewOffline` | Ski patrol/crew availability |
| **Badge Rarities** | `badgeCommon`, `badgeRare`, `badgeEpic`, `badgeLegendary` | Achievement badge tiers |
| **Glass Morphism** | `glassBackground`, `glassBorder` | Frosted glass UI effect |
| **Gradients** | `gradientStart`, `gradientMid`, `gradientEnd` | Gradient color stops |

### Custom Text Styles

Access via `TrailsTheme.textStyles`:
- `message`, `messageEmphasis` - Chat/message content
- `timestamp` - Time display formatting
- `codeInline`, `codeBlock` - Code formatting
- `input`, `button` - Form element text

## Icons (Hugeicons)

**Source:** `/Users/matt/Desktop/design/Hugeicons`

Icons are sourced from Hugeicons Pro. When adding new icons:

### 1. Find the SVG

```bash
# Search for an icon (e.g., "clock")
find /Users/matt/Desktop/design/Hugeicons -iname "*clock*" -type f | grep stroke-rounded
```

Use `Stroke Icons/Stroke/Rounded/{icon-name}-stroke-rounded.svg` for outlined icons.

### 2. Convert SVG to Android Vector Drawable

Create XML in `multiplatform/foundation/designsystem/src/commonMain/composeResources/drawable/`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:pathData="..." <!-- Copy from SVG 'd' attribute -->
      android:strokeLineJoin="round"
      android:strokeWidth="1.5"
      android:fillColor="#00000000"
      android:strokeColor="#000"
      android:strokeLineCap="round"/>
</vector>
```

**Naming:** `{icon_name}_stroke_rounded.xml` (e.g., `clock_02_stroke_rounded.xml`)

### 3. Register in Icons.kt

```kotlin
// 1. Add import
import trails.multiplatform.foundation.designsystem.generated.resources.{icon_name}_stroke_rounded

// 2. Add to Icons.Outlined object
val {IconName}
    @Composable
    get(): Icon = IconProvider(contentDescription = "{IconName}") {
        painterResource(Res.drawable.{icon_name}_stroke_rounded)
    }
```

### 4. Use in Components

```kotlin
import org.mobilenativefoundation.trails.foundation.designsystem.icon.Icons

val icon = Icons.Outlined.Clock
Icon(
    painter = icon.painter,
    contentDescription = icon.contentDescription,
    tint = TrailsTheme.colors.textSecondary,
    modifier = Modifier.size(16.dp)
)
```

### Available Icon Styles

| Style | Path | Use Case |
|-------|------|----------|
| Stroke Rounded | `Stroke Icons/Stroke/Rounded/` | Default outlined icons |
| Solid Rounded | `Stroke Icons/Solid/Rounded/` | Filled icons |
| Duotone Rounded | `Stroke Icons/Duotone/Rounded/` | Two-tone icons |
| Bulk Rounded | `Stroke Icons/Bulk/Rounded/` | Heavy filled icons |

## DI Scopes

| Scope | Lifetime | Use Case |
|-------|----------|----------|
| `AppScope` | Application | Singletons: repositories, HTTP client |
| `LoggedInScope` | User session | User-specific services |
| `ActiveScope` | Foreground | Screens, presenters, UI |
| `InactiveScope` | Background | Background tasks |
| `LoggedOutScope` | Pre-auth | Login/signup screens |

## Gradle Plugins

| Plugin | Applies | Use For |
|--------|---------|---------|
| `plugin.trails.kotlin.multiplatform` | KMP + Kover | Foundation, data modules |
| `plugin.trails.feature` | KMP + Compose + Metro + KSP | Screens, features |
| `plugin.trails.library` | Lightweight library | Scopes, simple models |

## Key Dependencies

- **Navigation**: [Circuit](https://slackhq.github.io/circuit/) by Slack
- **DI**: [Metro](https://zacsweers.github.io/metro/) by Zac Sweers
- **State**: [Atom](https://github.com/matt-ramotar/atom) by matt-ramotar - FSM-based state management
- **UI**: Compose Multiplatform + Material3
- **Network**: Ktor client
- **Serialization**: kotlinx.serialization

## Do's

- Use `@ContributesBinding(Scope::class)` for automatic DI wiring
- Keep Presenters as `@Composable` functions returning immutable State
- Include `send: (Intent) -> Unit` in State for actions
- Use `remember` and `mutableStateOf` for local state in Presenters
- Follow API/IMPL separation for all modules
- Add android namespace in every module's build.gradle.kts
- Put navigation destinations in `screen/`
- Put cross-cutting features in `feat/`
- Inject feature triggers into screen Presenters
- Use `TrailsTheme` for all theming (typography, colors, spacing, radii)
- Extract theme tokens to local variables at the top of composables
- Implement `CircuitUiState` on all State sealed interfaces
- Implement `CircuitUiEvent` on all Intent sealed interfaces
- Implement `Ui<State>` interface on all Ui classes

## Don'ts

- Don't put UI logic in Ui classes - use Presenters
- Don't skip DI registration in ActiveGraph
- Don't use constructor injection without `@Inject`
- Don't create screens without both api and impl modules
- Don't forget `@Parcelize` on Screen data classes
- Don't put reusable features in `screen/` - use `feat/` instead
- Don't register features with Circuit - inject them into Presenters
- Don't use `MaterialTheme` directly - use `TrailsTheme` instead
- Don't mix `Intent` (Atom) with `CircuitUiEvent` (Circuit) - use one pattern per module
