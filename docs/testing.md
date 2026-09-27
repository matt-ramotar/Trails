# Testing

Run one Gradle invocation at a time. Prepare the local libraries using
[dependency setup](dependency-setup.md). Its IDE-resolution check does not
compile or run the application.

## Local checks

Check module registration, declared production dependency cycles, implementation
boundaries, data-layer dependencies, source packages, and retired imports:

```bash
python3 scripts/check_architecture.py
```

The checker reads literal Gradle declarations and checked-in convention plugins.
It does not resolve external dependencies or replace compilation. Generated build
output and SQLDelight schema directories are excluded from Kotlin package checks.

Check direct Material visual-control usage against the shared adapter boundary:

```bash
python3 scripts/check_components.py
python3 -m unittest discover -s scripts/testing -p test_components.py
```

The component check catches known Material controls imported directly, aliased,
used through wildcard imports, or fully qualified outside the design system.
Text, Icon, native state APIs and annotations remain available. The only
layout-only `Scaffold` exception is `app/runtime`'s `AccountContent.kt`.
The checker is lexical and does not prove that custom surfaces or the adapters
themselves match HeroUI. Review the [component inventory](components.md) as well.

Run the relevant module's `jvmTest` and compile task. Navigation and host examples:

```bash
./gradlew :multiplatform:app:navigation:impl:jvmTest :multiplatform:app:runtime:jvmTest
./gradlew :apps:android:assembleDebug :apps:android:assembleDebugAndroidTest
```

Run the dependency guards, architecture-checker regression tests, and capture-reader
tests without Gradle:

```bash
python3 -m unittest discover -s integration-tests/store6-consumer -p '*_test.py'
python3 -m unittest discover -s scripts/testing -p 'test_*.py'
```

The [standalone consumer](../integration-tests/store6-consumer/README.md) covers
library integration and file-backed recovery separately from the application.
Keep the first failure. Report which tasks ran freshly, were cached, or did not
start. Do not infer installed behavior from compilation or source inspection.

## Component verification

For a shared-component change, inspect every affected use in the
[screen and feature inventory](components.md#screen-and-feature-inventory).
Compare geometry, typography roles, state appearance and motion against the
pinned Native source. A source-usage check does not establish this comparison.

Run the affected design-system, shared trail UI, screen, feature and runtime
module tests in one serialized Gradle invocation, followed by the Android build.
Retain tests for independent card/bookmark actions, description expansion,
toast callbacks/timeout/tap interception, all five navigation destinations,
scroll restoration, sync-state meaning and guarded save dismissal. Add focused
behavior tests when an adapter changes those contracts.

Capture installed screens at normal and 200% font scale using the following
matrix. Record the relevant state separately when one capture cannot show it.
An unavailable fixture or state is a gap to report, not a passed check.

| Surface | States and interactions |
| --- | --- |
| Welcome | Hero card and links; opening sample/loading action; failure and retry. |
| Explore | Search/clear/IME, filter triggers, sort popup, loaded cards, loading, no matches and recovery. |
| For You | Featured card, recommendation rows and hearts; loading, missing details and cached refresh failure. |
| Navigate | Trail-name action, schematic-label chip, persistent panel, placeholder-recording toast, loading and unavailable/retry. |
| Saved | Both tabs, collection tiles/placeholders, trail cards, empty, missing details and pending/failed sync. |
| Activity | Summary card, chart, hero and rows, independent hearts, loading, empty and cached recovery. |
| Collection | Back action, counts, cards, empty, unavailable, missing details and sync notices. |
| Trail Detail | Photo actions, content surface, facts/separators, chips, description expand/collapse, links, save and recovery. |
| Filters | All control families, min/max and upper-stop values, counting, count failure, applying and draft cancellation. |
| Save Trail | Loading/unavailable membership, collection rows, editable draft, admitting/reconciling, removal confirmation and completion toast. |
| Developer tools | Each tab, status chip, settings groups, controls, menus, pending/failed/unavailable runtime, copy/reset and account actions. |
| Runtime/account shell | Splash, configuration restoration, startup failure/retry, navigation-checkpoint failure and overlays above the current root. |

Check normal, pressed, focused, selected, disabled and loading appearance where
applicable. Verify system-disabled animations, keyboard traversal, focus return,
Android Back/Escape and modal isolation. Native range actions must expose labels,
units and bounds. Checkbox and switch rows must have one action each. A heart
inside a clickable card must invoke saving without opening the trail. Loading
must remain understandable when animation is disabled.

Retain narrow-phone navigation checks at 320 and 360 dp with 200% fonts, plus
the normal-text single-row case. Verify all labels and 48 dp action targets
remain visible and usable. Inspect longer labels and right-to-left layout where
the adapter uses directional geometry. Screen layout and custom drawing do not
become HeroUI components merely because their controls pass these checks.

Record the pinned reference SHA, application source/diff identity, dependency
manifest, local and installed APK hashes, device/OS, font and animation settings,
actions and results under ignored `build/verification/` output. Keep any
packaging difference separate from code/resource identity. Capture the original
account and developer/system settings and restore them after checks. Follow the
persistence acceptance below when adapting save, toast, state or overlay flows.

## Developer tools

Swipe right from inside the left side of the app to open the drawer. On Android
with gesture navigation, start beyond the system Back gesture strip. Close it
with the close button, Android Back, or Escape on a keyboard.

**Network** contains Offline, latency, error rate, and rate limit. **Sync** contains
conflict simulation. **Session** contains sample accounts, simulation seed, and
Activity refresh when Activity is the current destination. Settings save
automatically; **Applied** means the backend has accepted the saved configuration.
**Reset** restores saved defaults, and **Copy config** copies the supported backend
configuration as JSON. At large text sizes, scroll the form and the tab strip to
reach every control.

## Android persistence acceptance

Record source revision, dependency manifest, APK hash, device/OS, actions, and
results under a new ignored `build/verification/` directory. Select the device
from `adb devices`. The commands use `TRAILS_DEVICE` for that serial.

1. Install with `adb -s "$TRAILS_DEVICE" install -r apps/android/build/outputs/apk/debug/android-debug.apk`
   to preserve existing data. Open a known trail, apply a query, and verify
   returning from detail restores query and scroll.
2. Apply developer **Offline** and observe the applied state before saving.
   Airplane mode alone does not control the in-process backend.
3. Change a trail's collections and save. Verify local admission, pending status,
   shared hearts, collection counts, and unchanged membership after cancelling
   an unsubmitted draft.
4. Force-stop and relaunch without clearing data. Verify restored session,
   applied Offline setting, navigation, cached content, and pending mutation.
5. Apply Online. Verify convergence and exactly one additional logical backend
   effect, retaining earlier receipts and the other account's stored data.
6. Exercise save/unsave/save, local write failure, refresh failure, lost
   acknowledgement, and local adoption failure. Switch accounts during reads and
   pending work to verify isolation and complete retirement.

```bash
adb -s "$TRAILS_DEVICE" shell am force-stop org.mobilenativefoundation.trails.android
adb -s "$TRAILS_DEVICE" shell monkey -p org.mobilenativefoundation.trails.android -c android.intent.category.LAUNCHER 1
```

Check cold offline behavior in a separate fresh installation. It must show
unavailable/recovery states rather than a fabricated empty collection. Do not
clear data between the pending, restarted, and reconnected stages above.

Check every root's return state, Activity/For You sample feeds, and Navigate's
labelled schematic. Inspect 200% text scale and native control semantics.
[Accessibility instrumentation](../apps/android/src/androidTest/README.md)
requires an active TalkBack service and has narrower coverage than human speech
and full gesture review.

## Capture tools

`android_capture.py` selects an explicit device and writes original PNG bytes,
fresh native XML, and timestamped actions. It does not clear app data. Text input
is logged, so use it only for non-sensitive sample queries. Do not run it while
accessibility instrumentation owns the automation connection.

```bash
python3 scripts/testing/android_capture.py --serial "$TRAILS_DEVICE" \
  --output-dir build/verification/android capture explore
```

Other actions are `nodes`, `tap`, `slide`, `text`, `key`, `wait`, and
`pull`. `pull` reads an existing `/sdcard/<name>.png` and `.xml` pair created by
a persistent device shell. Set `--adb` or `--port` when using another SDK/server.
Existing capture names are rejected. A PNG/XML pair records consecutive reads,
not an atomic frame.

`database_snapshot.py` reads a copied databases/preferences tar without device
access. It bounds extraction, retains copied WAL files, opens SQLite read-only,
and hashes account identifiers and query text. It reports catalog rows,
collection membership, mutation state, account feeds, and view checkpoints.

```bash
python3 scripts/testing/database_snapshot.py snapshot.tar \
  --output build/verification/snapshot.json --capture-state force-stopped
```

The caller declares the capture state. The reader cannot prove that the app
was stopped. Record the force-stop and absent process before copying the
archive. Per-file integrity does not prove an atomic multi-database snapshot.
For reproducible activity totals, supply `--as-of YYYY-MM-DD` and
`--timezone America/New_York` using the capture's date and device time zone.
Snapshot summaries are observations, not automatic acceptance results.
