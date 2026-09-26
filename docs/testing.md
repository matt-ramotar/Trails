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
