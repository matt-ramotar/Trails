# Architecture

Circuit coordinates screens, Metro constructs dependency graphs, Compose renders
UI, Store6 persists data, and Atom manages save drafts through local submission.

## Modules

| Layer | Responsibility |
| --- | --- |
| `apps/android` | Android application, lifecycle, and debug/test entry points |
| `multiplatform/app/runtime` | Application host, session bootstrap, account retirement, and dependency graphs |
| `multiplatform/app/navigation` | Navigation contracts, independent root stacks, and account view checkpoints |
| `multiplatform/screen` | Circuit destinations with presenters, state/events, and UI |
| `multiplatform/feature` | Reusable filtering and save interactions invoked by screens |
| `multiplatform/ui/trail` | Trail cards, facts, save indicators, and shared list presentation |
| `multiplatform/data` | Repository contracts and persisted implementations |
| `multiplatform/foundation` | Design system, scopes, logging, dispatchers, and platform primitives |
| `integration-tests/store6-consumer` | Standalone tests for dependency integration and persistence |

Split `api` and `impl` when another module needs the contract independently of
its implementation. Private utilities and infrastructure can remain single
modules. Screens may depend on feature contracts. Features cannot depend on
their invoking screens. Shared UI depends on data contracts and visual
primitives, not on a save implementation or an application graph.

Production dependencies on `impl` modules belong to `app/runtime` and platform
app hosts. Other modules consume contracts. Tests may depend on implementations.
Data modules use the library convention and optional DI/storage plugins without
Compose or Circuit. The declared production module graph must remain acyclic.

Kotlin packages follow the owning module under
`org.mobilenativefoundation.trails`. Source directories and Android namespaces
follow the same structure. Public models belong to their domain.

## Bootstrap and accounts

The session repository exposes the current user and a stream of restored state.
Bootstrap waits for the stored backend configuration before opening account
repositories. It routes signed-out users to Welcome, users with incomplete
onboarding to PreLanding, and active users to the application roots.

On an account transition, bootstrap removes private UI, cancels and joins the
old account's jobs, and closes its drivers before creating the next graph. A
failed retirement can be retried. Driver closure cannot be skipped.
Late results from an old account cannot enter the new account's state.

`data/session/api` owns `UserRepository`, sample account identities, and session
models. `data/session/impl` owns persistence behind that contract.
A missing session is distinct from a storage failure. Store6 provides local
reads and writes through a `LocalOnly` store backed by SQLDelight.
At the pinned revision, [`Freshness.LocalOnly`](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/core/src/commonMain/kotlin/org/mobilenativefoundation/store6/core/Freshness.kt)
reads local rows without fetching. Session writes use
[`StoreWriteHandle.apply`](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/core/src/commonMain/kotlin/org/mobilenativefoundation/store6/core/seam/StoreWriteHandle.kt)
to commit before publishing. `current` updates through the observed stream,
so callers must not assume it changes synchronously when `persist` returns.
Session persistence has no remote authentication, mutation journal, or freshness
metadata. `data/database` owns the shared `TrailsDatabase`, platform driver
factories, and `user_state`/`developer_settings` queries. Its installed database
name remains `trails.db`. Legacy tables remain for compatibility.

`data/backend` owns the fake backend configuration types. `data/developersettings`
persists those controls. `data/trail` owns the fake backend behavior alongside the
trail repositories that consume it.

## Persistent data and save flow

The shared catalog stores canonical trail records and ordered query memberships.
Account value storage contains saved memberships, collections, recommendations,
and sample activities. A separate mutation journal retains pending desired-state
saves and admission receipts. Closing or restarting the application does not
replace these persistent stores with transient UI state.

The fake backend persists its own configuration, saved membership, operation
receipts, and account fixtures across client restarts. Membership updates and
their idempotency receipts commit together. Catalog reseeding invalidates cached
catalog data before repositories start, preserving account work and backend
receipts.

The save feature holds editable choices and freezes them on submission. Its Atom
runs a bounded local operation: admitting the save or checking whether an
uncertain admission committed. The account service handles recovery, sends
queued mutations to the backend, and applies acknowledged results to local
storage, including retry backoff. An
uncertain admission must be resolved using the same command identity and payload
before another command can be created. See [behavior](behavior.md) for UI outcomes.

## Navigation and platform boundaries

Each of the five roots owns a back stack. Account checkpoints retain root,
query text, applied filters, selected Saved segment, opened trail, and scroll
positions. Checkpoint serialization is versioned and retains defaults for roots
absent from older checkpoints. Domain data never belongs in these checkpoints.

Android persists view checkpoints. In-memory platform fallbacks do not promise
process-death restoration. The trail repositories' pinned
[`SqlDelightSourceOfTruth`](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/sqldelight/src/commonMain/kotlin/org/mobilenativefoundation/store6/sqldelight/SqlDelightSourceOfTruth.kt)
supports synchronous transactions only. JavaScript durable storage is
unsupported. JVM tests and resolution of iOS/JS
publications do not establish application behavior on those platforms.

Installed databases and preference filenames retain their existing
`trails-m1-` prefixes for compatibility. Those storage identifiers do not govern
source class or package names.
