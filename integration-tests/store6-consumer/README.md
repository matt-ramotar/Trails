# Store6 and Atom consumer fixture

This standalone JVM/Android fixture exercises generated SQLDelight queries,
file-backed Store6 reads and mutation journals, command admission/reconciliation,
and a finite Atom save flow. The Android host uses Circuit, Compose, and a
generated Metro graph. Its package, databases, and controls are separate from
the Trails app.

The Android fixture retains application ID
`org.mobilenativefoundation.trails.c3` for installed-data compatibility.

## Prepare and run

Follow [dependency setup](../../docs/dependency-setup.md) to prepare the pinned
sources and isolated local publications. The fixture requires JVM/Android
publications. Omit `--trails-targets` only when preparing exclusively for this
fixture. An all-target candidate also satisfies the fixture.

From the repository root:

```bash
python3 -m unittest discover -s integration-tests/store6-consumer -p '*_test.py'
./gradlew -p integration-tests/store6-consumer jvmTest :android-preview:assembleDebug
```

The Python suite uses synthetic source records and artifact bytes to test guard
behavior. It does not validate the Kotlin libraries. The JVM suite uses new
file-backed databases and checks admission, reopening, account isolation,
acknowledgement/adoption failure, and operation deduplication.

## Guard contract

`prepare.py` verifies clean immutable sources, the pinned provenance bundle,
version agreement, and every required artifact hash without cloning, publishing,
or invoking Gradle. Source-only or invalid preparation revokes an older candidate.
`--check-candidate` is read-only even when validation fails.

Gradle settings and the pre-compilation `verifyCandidate` task recheck the
candidate. Configuration caching is disabled in this fixture. The candidate
pairs a generated manifest with its SHA-256. The selected libraries resolve
exclusively from the named repository outside ambient Maven Local. Root KMP
metadata JARs are required alongside platform binaries, POMs, and module files.

Atom construction is manual. KSP without the Atom processor does not
establish generated-factory or incremental-compiler compatibility. Passing this
fixture does not establish installed Trails behavior or iOS/JavaScript runtime
support.
