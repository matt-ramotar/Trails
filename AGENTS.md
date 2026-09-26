# Trails contributor instructions

Trails is a Kotlin Multiplatform sample with an Android host. Read
[architecture](docs/architecture.md) for module boundaries and
[behavior](docs/behavior.md) for the application contracts.

## Module and package placement

- Put full-screen Circuit destinations in `multiplatform/screen/{name}`.
- Put reusable interactions, such as filtering and saving, in
  `multiplatform/feature/{name}`. Keep screens and features separate.
- Use `api`/`impl` modules when callers need a stable contract without the
  implementation. Do not split infrastructure or a private implementation just
  to make every directory look alike.
- Keep shared trail presentation in `multiplatform/ui/trail`, reusable visual
  primitives in `multiplatform/foundation/designsystem`, and app wiring in
  `multiplatform/app/runtime`.
- Match source directories, Kotlin packages, and Android namespaces. The root
  namespace is `org.mobilenativefoundation.trails`; module layers are reflected
  below it. Use lowercase domain names rather than milestone, task, or revision
  names.
- Dependencies point from app composition and screens toward feature contracts,
  data contracts, and foundations. Features and data must not depend on screens
  or app composition. Keep implementation dependencies out of public APIs.
- Only `app/runtime` and platform app hosts consume `impl` modules in production;
  test dependencies may use implementations. Data modules use the library
  convention with optional DI/storage plugins and must not depend on UI.

## State and lifetime

Presenters produce immutable Circuit state and handle screen events. UI classes
render state and forward events. Use `TrailsTheme` tokens and native Compose
semantics for controls.

Store6 owns persisted reads and durable mutations. Session state restores before
account repositories open. Account services own draining, retries, and driver
closure; retirement must cancel and join old work before another account opens.
Atom owns the finite save draft/admission flow. Do not collect unbounded streams
or wait for remote settlement inside its sequential interpreter.

Navigation checkpoints contain view state only. Keep installed database names,
preference keys, serialized values, and Android application IDs compatible unless
the change includes an explicit migration. A source rename is not permission to
reset storage.

## Verification

Prepare dependencies using [dependency setup](docs/dependency-setup.md) before
Gradle configuration. Run one Gradle invocation at a time across Trails and its
local dependency producers.

Run `python3 scripts/check_architecture.py` after changing modules, dependencies,
or source placement. It checks the declared graph and authored source without
starting Gradle.

Validate edits with the smallest relevant module compile or test task. Run the
Android build when changing host wiring, module dependencies, or shared UI:

```bash
./gradlew :apps:android:assembleDebug
```

Use file-backed databases for persistence tests and reopen drivers to verify
recovery. Installed Android force-stop/relaunch is a separate process-lifecycle
check. Follow [testing](docs/testing.md) for that acceptance sequence.

Preserve the first failing output before a repair. Do not rerun an unchanged
failure to obtain green. A wrapper or configuration failure before test startup
means zero tests ran. Distinguish fresh test execution, cached results,
compilation, installed behavior, and platform dependency resolution in reports.
Write generated logs and captures under ignored build output.

## Documentation and assets

Document only what a contributor needs to use, change, or reason about the
system. Keep current setup and contracts in `docs/`; do not commit task plans,
completion narratives, screenshots, or execution transcripts. Git preserves
previous tracked content.

Comments should explain non-obvious contracts, invariants, or reasons. Remove
organizational labels and comments that repeat adjacent code. Preserve exact
commands, technical identifiers, units, limitations, and compatibility contracts
when revising prose.

Keep asset licenses and source attribution. Use checked-in resources or a
reproducible source; a developer's local directories and expiring image URLs are
not build inputs.
