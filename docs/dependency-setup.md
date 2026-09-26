# Local dependencies for Gradle sync

Trails resolves Store6 and Atom exclusively from an isolated local Maven repository. Their root multiplatform metadata refers to separate platform publications. JVM/Android artifacts alone let a JVM compile succeed while IDE sync fails on iOS and JavaScript source sets.

Use the immutable Store6 revision `582edfe86e64ddc71312ecd20a1895fc3de37b52` and Atom revision `05daa800ac3c6d0dc1f9538234b99061c8139e40`. Keep the source clones and Maven repository under the checkout's ignored `.gradle/c3-dependencies/` directory. The original `/private/tmp/trails-c3-20260915` directory is historical and may no longer exist.

The commands require Python 3, Java 17, an Android SDK, and Xcode for iOS publication. Set `ANDROID_HOME` to the installed SDK. Use each producer's checked-in Gradle wrapper and toolchains, and run producers serially. Publication is local to the specified repository.

## Prepare immutable sources

From the Trails root, set `STORE6_SOURCE` and `ATOM_SOURCE` to Git repositories containing those commits. For a new setup:

```bash
TRAILS_ROOT="$PWD"
TRAILS_C3_ROOT="$TRAILS_ROOT/.gradle/c3-dependencies"
mkdir -p "$TRAILS_C3_ROOT"
git clone --no-checkout --no-hardlinks "$STORE6_SOURCE" "$TRAILS_C3_ROOT/store6"
git -C "$TRAILS_C3_ROOT/store6" checkout --detach 582edfe86e64ddc71312ecd20a1895fc3de37b52
git clone --no-checkout --no-hardlinks "$ATOM_SOURCE" "$TRAILS_C3_ROOT/atom"
git -C "$TRAILS_C3_ROOT/atom" checkout --detach 05daa800ac3c6d0dc1f9538234b99061c8139e40
```

Reuse existing clean clones at those revisions when repairing a setup. Do not replace their committed source with files from a shared working tree. Verify the source and recorded owner handoff before publication:

```bash
python3 integration-tests/store6-consumer/prepare.py \
  --store6-source "$TRAILS_C3_ROOT/store6" \
  --atom-source "$TRAILS_C3_ROOT/atom" \
  --atom-revision 05daa800ac3c6d0dc1f9538234b99061c8139e40 \
  --atom-version 0.1.0-SNAPSHOT \
  --owner-handoff docs/evidence/m1/dependencies/atom-owner/handoff.json \
  --repository "$TRAILS_C3_ROOT/maven"
```

This source-only step revokes an older candidate. It does not enable Gradle configuration.

## Publish every Trails target

The root, JVM, Android, `iosArm64`, `iosSimulatorArm64`, `iosX64`, and JS publications are required for each selected module. Preserve producer logs alongside the generated manifest.

```bash
(
  cd "$TRAILS_C3_ROOT/store6" || exit
  for module in core sqldelight mutations mutations-sqldelight; do
    ./gradlew -Dmaven.repo.local="$TRAILS_C3_ROOT/maven" \
      ":$module:publishKotlinMultiplatformPublicationToMavenLocal" \
      ":$module:publishJvmPublicationToMavenLocal" \
      ":$module:publishAndroidReleasePublicationToMavenLocal" \
      ":$module:publishIosArm64PublicationToMavenLocal" \
      ":$module:publishIosSimulatorArm64PublicationToMavenLocal" \
      ":$module:publishIosX64PublicationToMavenLocal" \
      ":$module:publishJsPublicationToMavenLocal" || exit
  done
)
(
  cd "$TRAILS_C3_ROOT/atom" || exit
  for module in core compose; do
    ./gradlew -Dmaven.repo.local="$TRAILS_C3_ROOT/maven" \
      ":$module:publishKotlinMultiplatformPublicationToMavenLocal" \
      ":$module:publishJvmPublicationToMavenLocal" \
      ":$module:publishAndroidReleasePublicationToMavenLocal" \
      ":$module:publishIosArm64PublicationToMavenLocal" \
      ":$module:publishIosSimulatorArm64PublicationToMavenLocal" \
      ":$module:publishIosX64PublicationToMavenLocal" \
      ":$module:publishJsPublicationToMavenLocal" || exit
  done
)
```

After both producers succeed, repeat preparation with `--verify-artifacts --trails-targets`:

```bash
python3 integration-tests/store6-consumer/prepare.py \
  --store6-source "$TRAILS_C3_ROOT/store6" \
  --atom-source "$TRAILS_C3_ROOT/atom" \
  --atom-revision 05daa800ac3c6d0dc1f9538234b99061c8139e40 \
  --atom-version 0.1.0-SNAPSHOT \
  --owner-handoff docs/evidence/m1/dependencies/atom-owner/handoff.json \
  --repository "$TRAILS_C3_ROOT/maven" \
  --verify-artifacts --trails-targets
python3 integration-tests/store6-consumer/prepare.py --check-candidate --trails-targets
```

This records 126 required POM, Gradle metadata, and binary files in `integration-tests/store6-consumer/build/preparation/manifest.json` and writes the ignored `candidate.properties`. Production settings reject missing files, unrecorded platform publications, or changed recorded bytes. The standalone C3 fixture retains its JVM/Android-only default when `--trails-targets` is omitted.

## Verify IDE dependency resolution

```bash
./gradlew --no-configuration-cache \
  -I integration-tests/store6-consumer/verify-ide-sync.init.gradle \
  verifyC3SyncDependencies
```

The check uses Kotlin's IDE resolver for every source set and separately resolves Android compile dependencies. It fails on unresolved dependencies and writes `build/reports/dependency-sync.json`. It verifies dependency resolution, not iOS/JS application compilation, linking, or runtime behavior.
