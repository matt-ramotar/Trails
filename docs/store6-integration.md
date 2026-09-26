# M1 joint Store6 and Atom integration

**September 16, 2026 — bounded C3 passed for JVM and Android; production M1 is installed and accepted within its Android scope.** The standalone consumer passed 13 file-backed persistence/admission tests. Its Android package demonstrated Offline save, force-stop/reopen, an in-place schema upgrade retaining pending work, and reconnect with one backend effect. Atom core/Compose passed 288 fresh tests on the joint tuple; Metro adapters compiled. These dependency results are separate from [production acceptance](evidence/m1/continuation/README.md). The continuation reverified unchanged producer/artifact hashes and captured the actual Android runtime graph; it did not rerun the unchanged C3 suites.

For production Gradle/IDE sync, follow [local dependency setup](dependency-setup.md). Trails also declares iOS and JavaScript targets, so its settings require `prepare.py --check-candidate --trails-targets`. A candidate prepared only for the JVM/Android fixture cannot satisfy production sync.

## Reproducible sources

| Input | Immutable revision | Consumption |
| --- | --- | --- |
| Trails checkpoint | `86209d747fae68cc0f6081aa736a26b02b39a249`, then `2a41dd56e514d34cf910e07679df7ca7791c5edf` | Preserved on `matt-ramotar/v2.5`; production edits are uncommitted |
| Store6 | `582edfe86e64ddc71312ecd20a1895fc3de37b52` | Clean detached clone; core, sqldelight, mutations and mutations-sqldelight JVM/Android publications |
| Atom implementation | `05daa800ac3c6d0dc1f9538234b99061c8139e40` | Clean detached clone; core and compose JVM/Android publications |
| Atom owner evidence | `36307c6571525a05926fb642c93f7151ea3c7cbb` | [Explicit handoff and released Gradle lease](evidence/m1/dependencies/atom-owner/handoff.json); 152 source hashes matched |

The shared Atom and Store6 directories are not consumed. Producers write only to `/private/tmp/trails-c3-20260915/maven`, never ambient Maven Local. [Artifact hashes](evidence/m1/dependencies/artifact-manifest.json), [source manifest](evidence/m1/dependencies/source-manifest.json), and [reproduction instructions](evidence/m1/dependencies/REPRODUCTION.md) record provenance. Eight preparation-guard tests cover dirty sources, altered artifacts and stale candidates. The production settings file invokes the guard and resolves only the selected modules exclusively from that repository; legacy Store5 still resolves independently.

The exact Store6 documentation helper returned `VERSION_MISMATCH`; [first evidence](evidence/m1/dependencies/documentation-helper-first-failure.json) is retained. Immutable source KDoc and README supplied the applicable contracts. The recorded Maven Central 404 responses apply only to the checked metadata endpoints.

## Executed consumer tuple

| Component | Version |
| --- | --- |
| Kotlin / Compose compiler | `2.3.20` |
| Compose Multiplatform / Material3 | `1.9.1` / `1.9.0` |
| KSP / Metro | `2.3.10` / `0.11.3` |
| Circuit / SQLDelight | `0.30.0` / `2.1.0` |
| AGP / Gradle / Java | `8.12.3` / `8.13` / `17` |
| Coroutines / serialization | `1.10.2` / `1.9.0` |
| Store6 / Atom | `6.0.0-SNAPSHOT` / `0.1.0-SNAPSHOT`, at the immutable revisions above |

The Atom producer retained its owner's tuple. A separate test-only clone changed only the five catalog pins recorded in [the patch](evidence/m1/dependencies/atom-joint-tests-catalog.patch); no artifacts were published from that modified clone. [Joint revalidation](evidence/m1/dependencies/atom-joint-results/summary.json) distinguishes the executed tests from owner-reported evidence. KSP is applied, but the consumer uses a manual Atom factory; generated-Atom/incremental KSP support is not established by this gate.

## Executed persistence and runtime proof

- Actual generated SQLDelight queries, `SqlDelightSourceOfTruth`, `SqlDelightBookkeeper`, and `SqlDelightMutationJournalStorage` use file databases. Value and journal drivers are separate.
- Offline desired membership survives closing every driver and reopening. Account namespaces and a persisted journal-installation UUID prevent Store6's local `client-0` sequence from colliding across accounts or installations.
- Fake-server settings, authoritative membership and operation receipts persist. The authoritative update and receipt commit together. Lost acknowledgement replays the same operation without a second application.
- An acknowledged mutation whose local adoption fails reopens and finishes offline without another backend push.
- A delicate journal decorator writes the client command receipt and assigned mutation ID in the same SQL transaction as admission. Duplicate submissions, changed payloads, lost caller results, post-pruning reconciliation, and receipt-write rollback are covered.
- The Android fixture consumes real Circuit/Compose wiring through a generated Metro graph. [Database proof](evidence/m1/dependencies/android-fixture/database-proof.json) records one pending mutation while Offline and exactly one settled backend receipt/effect after reconnect; data was not cleared.

The final fixture run is [13 tests, zero failures](evidence/m1/dependencies/consumer-atomicity-results). [First attempts](evidence/m1/dependencies/consumer-attempts.json) preserve configuration, compilation and runtime failures before repairs. Global drain honors persisted backoff; keyed drain is an explicit retry. SQLDelight JDBC temporary triggers do not survive its outside-transaction connection lifecycle, so failure-window tests use persistent triggers and explicitly remove them.

## Production ownership

`data/trail` owns persisted ordered query results, trail details, saved memberships, command receipts and durable mutations. Its account service owns admission jobs, bounded drain passes, retries, cancellation/join and driver closure. `feat/savetrail` uses repaired Atom for the finite editing/admitting/reconciling flow. It cannot substitute transient Atom state for journal recovery.

The unused legacy TODO `PostAtom` and alpha02 generated registry path are retired. Existing legacy user/session and ski modules remain on Store5 during M1; the final M4 contract still requires Store5's removal. Restored developer settings must be applied to both fake services before opening an M1 account, and restored session streams must not emit a synthetic signed-out state first.

## Remaining boundaries

The production M1 APK and all nine completion-script cells are tracked in [acceptance.json](evidence/m1/acceptance.json), with cumulative per-build acceptance and [the latest installed restart proof](evidence/m1/continuation/README.md). The September 16 C3 evidence covers only JVM/Android dependency variants. Production IDE sync additionally requires the iOS/JS publications described in the setup guide; resolving them does not establish application execution on those platforms. The synchronous persistence adapter cannot use the existing asynchronous WebWorkerDriver; the new Web driver reports that unsupported capability explicitly. No Apple, Web, desktop-launcher, map, background recording, or release claim follows from C3.

Later maps, Moments, recording, activity, profile and platform milestones remain open.
