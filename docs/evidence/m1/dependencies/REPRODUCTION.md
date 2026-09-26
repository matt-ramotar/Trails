# Execute the C3 candidate after the Atom handoff

**Status: bounded JVM/Android C3 passed.** Clean producers, 13 consumer persistence/admission tests, 288 joint Atom tests and the installed Android fixture are recorded in [the integration report](../../../store6-integration.md). Atom's owner released the build lease and supplied exact source evidence; the coordinator matched all 152 hashes. Commands below reproduce that bounded dependency gate. Production M1 acceptance remains separate. Set `ANDROID_HOME` to the installed SDK if needed.

## 1. Capture the handoff

Obtain the owner-approved full Atom Task4 commit, coherent version, platform/API/direct-review records and released build lease. Record these in `source-manifest.json` before consuming the candidate. Task3's `cf98ac...` evidence commit is insufficient for the new `core`/`compose` layout. Save the exact received owner message and referenced verification evidence, then create a coordinator handoff record from `integration-tests/store6-consumer/owner-handoff.example.json`. Set `status` to `READY_FOR_C3_SOURCE_CONSUMPTION` only after the owner actually supplies that approval and releases the build lease. Fill the full revision/version, owner thread, received timestamp and SHA-256 hashes of the message/evidence files. Relative evidence paths resolve against the handoff JSON. The script checks consistency and bytes, not authorship or test sufficiency. Required candidate source APIs are described in `docs/store6-integration.md`; any final API change must be reconciled before compilation.

Use clean detached clones. Clone/fetch from the source repository or a local Git object database, never copy a dirty working tree. A local immutable Atom commit need not be publicly published to support a local reproducible fixture, but CI will need access to those exact objects or a separately governed artifact source.

The root coordinator has already prepared `/private/tmp/trails-c3-20260915/store6` at `582edfe86e64ddc71312ecd20a1895fc3de37b52` and `/private/tmp/trails-c3-20260915/atom` at `05daa800ac3c6d0dc1f9538234b99061c8139e40`. Recheck their HEAD/status and reuse these isolated clones after owner approval; the clone commands below are for a fresh run directory, not instructions to replace existing clones.

Example preparation, with `TRAILS_C3_ATOM_REVISION` explicitly set to the owner-provided full SHA:

```bash
: "${TRAILS_C3_ATOM_REVISION:?Set the owner-approved immutable Atom Task4 revision}"
: "${TRAILS_C3_ATOM_VERSION:?Set the coherent version from that source catalog}"
: "${TRAILS_C3_OWNER_HANDOFF:?Set the coordinator handoff JSON path}"
TRAILS_C3_ROOT=/private/tmp/trails-c3-20260915
mkdir -p "$TRAILS_C3_ROOT"
git clone --no-checkout --no-hardlinks /Users/matt/src/matt-ramotar/Store6 "$TRAILS_C3_ROOT/store6"
git -C "$TRAILS_C3_ROOT/store6" checkout --detach 582edfe86e64ddc71312ecd20a1895fc3de37b52
git clone --no-checkout --no-hardlinks /Users/matt/src/matt-ramotar/atom "$TRAILS_C3_ROOT/atom"
git -C "$TRAILS_C3_ROOT/atom" checkout --detach "$TRAILS_C3_ATOM_REVISION"
python3 integration-tests/store6-consumer/prepare.py \
  --store6-source "$TRAILS_C3_ROOT/store6" \
  --atom-source "$TRAILS_C3_ROOT/atom" \
  --atom-revision "$TRAILS_C3_ATOM_REVISION" \
  --atom-version "$TRAILS_C3_ATOM_VERSION" \
  --owner-handoff "$TRAILS_C3_OWNER_HANDOFF" \
  --repository "$TRAILS_C3_ROOT/maven"
```

This uses immutable committed objects and leaves both shared library checkouts untouched. `prepare.py` rejects a dirty clone or wrong HEAD and verifies that the Atom repair is an ancestor and optional modules are present. It records the source tree IDs and the coordinator handoff. Any failed or source-only preparation revokes old candidate properties. This source-only pass does not enable consumer configuration.

## 2. Build isolated artifacts with one runner

Use Store6's checked-in wrapper/toolchain for the Store6 producer and the accepted Atom producer wrapper/toolchain for Atom. Write each full command, start/end time, source SHA/status, exit status and complete log under a new run directory. Preserve the first red log/XML before any repair or retry. A wrapper rejection before startup means zero tests executed.

Inspect the producer's actual publishing task names first. Expected JVM/Android/root tasks from the inspected conventions are below; if a final handoff exposes different task names, record their discovered names rather than treating this recipe as execution evidence.

At Atom `05daa800`, `core` and `compose` apply the common publishing convention and declare `group = "dev.mattramotar.atom"` and `version = libs.versions.atom.get()`. The source workflow uses `:<module>:publishAndReleaseToMavenCentral`; that public release task is outside this recipe. Local task names below were discovered from both immutable producers and executed successfully. Its [plugin implementation](https://github.com/vanniktech/gradle-maven-publish-plugin/blob/691e3ec7780219299b633037c02762493e43fa06/plugin/src/main/kotlin/com/vanniktech/maven/publish/MavenPublishBaseExtension.kt) chooses the `release` Android variant by default and makes signing required only for non-SNAPSHOT versions. The current Atom version is a SNAPSHOT; no signing check is disabled. Store6 explicitly configures the `release` variant. The [API review record](atom-05daa800-api-review.json) includes exact publication-source digests and expected task names.

```bash
./gradlew :core:tasks --all
```

Use each producer's own wrapper for this discovery and later publication. Atom uses Gradle `8.13`, Kotlin `2.2.20`, Compose `1.9.1`, and Java toolchain `17`; Store6 uses Gradle `8.11.1`, Kotlin `2.3.20`, and Java toolchain `11`. The consumer's provisional Kotlin `2.3.20` tuple does not silently replace either producer tuple.

Run in the clean Store6 clone, with the absolute task-specific repository value substituted:

```bash
./gradlew -Dmaven.repo.local=/private/tmp/trails-c3-20260915/maven \
  :core:publishKotlinMultiplatformPublicationToMavenLocal \
  :core:publishJvmPublicationToMavenLocal \
  :core:publishAndroidReleasePublicationToMavenLocal \
  :sqldelight:publishKotlinMultiplatformPublicationToMavenLocal \
  :sqldelight:publishJvmPublicationToMavenLocal \
  :sqldelight:publishAndroidReleasePublicationToMavenLocal \
  :mutations:publishKotlinMultiplatformPublicationToMavenLocal \
  :mutations:publishJvmPublicationToMavenLocal \
  :mutations:publishAndroidReleasePublicationToMavenLocal \
  :mutations-sqldelight:publishKotlinMultiplatformPublicationToMavenLocal \
  :mutations-sqldelight:publishJvmPublicationToMavenLocal \
  :mutations-sqldelight:publishAndroidReleasePublicationToMavenLocal
```

Run in the clean Atom clone after confirming its actual publication names:

```bash
./gradlew -Dmaven.repo.local=/private/tmp/trails-c3-20260915/maven \
  :core:publishKotlinMultiplatformPublicationToMavenLocal \
  :core:publishJvmPublicationToMavenLocal \
  :core:publishAndroidReleasePublicationToMavenLocal \
  :compose:publishKotlinMultiplatformPublicationToMavenLocal \
  :compose:publishJvmPublicationToMavenLocal \
  :compose:publishAndroidReleasePublicationToMavenLocal
```

These are file-repository development publications, not Maven Central/public publication. Confirm that the isolated repository was empty before the first producer. A source label and old artifact bytes do not form a reproducible candidate. If producer configuration/signing/toolchain fails, retain that blocker; do not silently modify or disable checks in the accepted source. Any producer patch becomes a separately recorded source delta requiring renewed source identity and verification.

Repeat `prepare.py` with the same source arguments plus `--verify-artifacts`. It checks expected root/JVM/Android `.pom`, `.module`, `.jar`/`.aar` files, records SHA-256 hashes and writes ignored `candidate.properties`. Preserve its `build/preparation/manifest.json` beside producer logs. Candidate properties also pin the manifest SHA-256. Settings validates that pairing, handoff records and artifact bytes, and the `verifyCandidate` task repeats the check before compilation. Configuration caching is disabled in the fixture. Hash checking records the observed bytes; only pairing with the clean producer execution establishes their provenance.

The recipe above publishes only JVM/Android variants for the standalone consumer. Production Trails also declares iOS and JS targets. Follow [local dependency setup](../../../dependency-setup.md) to publish those variants and prepare with `--verify-artifacts --trails-targets` before production IDE sync. Root KMP metadata references alone do not prove that platform binaries exist.

## 3. Execute the consuming checks

From the Trails root, use its Gradle 8.13 wrapper with the standalone project directory. Candidate properties must already exist; configuration otherwise fails closed.

```bash
./gradlew -p integration-tests/store6-consumer jvmTest :android-preview:assembleDebug
```

The original five `Store6PersistenceTest` cases are below; the final suite adds cross-account/install receipt identity, ACKED local adoption, and five `AdmissionRecoveryTest` cases for durable correlation and atomic rollback, for 13 total:

1. Seed a confirmed value, apply persisted fake-backend Offline, enqueue through the finite Atom, close the Atom/store/all SQL drivers, reopen the same disk files, verify restored optimistic projection and identical pending mutation ID, reconnect/drain, and reopen again to verify confirmed saved data with no queued work.
2. Persist the server value and receipt, lose the acknowledgement, close/reopen every driver and drain again. The authoritative apply counter must remain one.
3. A second account uses another value/journal database and sees no first-account pending work; the original account retains it. Construct the actual Metro graph used by the Circuit presenter/UI.

4. Return a known durable admission, force subsequent status inspection to fail, and prove the original mutation remains journaled after reopen.
5. Throw from the local admission boundary, leave enqueueing with the same uncertain command ID, and prove another queued request does not automatically replay it.

The JVM tests do not execute Android's Compose adapter lifecycle. The actual Android host registers a parcelable `FixtureScreen` in a `Circuit` built from the Metro-provided presenter/UI, and renders `CircuitContent` inside the Circuit/Atom composition providers. Its execution is the complementary consuming proof. Inspect resolved runtime/compile graphs and preserve reports; the fixture must resolve both library groups exclusively from the recorded repository. Separately rerun the relevant changed Atom adapter/runtime checks against the agreed tuple and record retained generated-binding compatibility in Trails before closing Gate A/C3.

## 4. Install and exercise the Android fixture

After assembly succeeds, the runner may install without replacing the production Trails package:

```bash
./gradlew -p integration-tests/store6-consumer :android-preview:installDebug
adb -s emulator-5554 shell am start -n org.mobilenativefoundation.trails.c3/org.mobilenativefoundation.trails.integration.preview.MainActivity
```

Select an actually available emulator serial rather than assuming this recorded example remains current. On a new fixture install, allow initial data loading, press **Apply Offline**, then **Save to Weekend**. Confirm a saved projection and one pending mutation. Force-stop, preserving all data:

```bash
adb -s emulator-5554 shell am force-stop org.mobilenativefoundation.trails.c3
adb -s emulator-5554 shell am start -n org.mobilenativefoundation.trails.c3/org.mobilenativefoundation.trails.integration.preview.MainActivity
```

Confirm Offline remains applied, the saved projection/pending work return, and the transient Atom starts a fresh lifetime. Press **Reconnect and drain**. Confirm pending becomes zero and saved remains true after another force-stop/relaunch. Capture screens, command logs and before/after database evidence. No data clear, uninstall, scenario reset or airplane-mode substitute is part of this scenario.

This fixture uses its own durable fake server, not Trails' existing fake backend implementation. Passing it would establish the bounded integration path only. Production M1 still requires adapting Trails' backend/persistence/services/UI and executing the completion contract in the current Trails APK.

## Preparation guard checks

`PYTHONDONTWRITEBYTECODE=1 python3 integration-tests/store6-consumer/prepare_test.py` executed eight synthetic Python tests successfully after the review fixes. They exercise stale-candidate revocation and manifest/artifact/handoff tampering with temporary fake source records and artifact bytes. These are preparation-script results only: zero Kotlin/Gradle tests or Android executions are implied.
