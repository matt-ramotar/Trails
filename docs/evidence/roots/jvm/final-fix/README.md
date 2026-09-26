# Final review fix wave — JVM evidence

Base: `482999374a798bde7d0b12225afa14abee7afb7b`. The working-tree source bytes tested by this gate are identified by [tested-source.json](tested-source.json) and [tested-source.patch](tested-source.patch). Canonical source manifest SHA-256: `b87b9a7a239542c1a714b76315ba6e3dc7aa6c9ef63c01b9dfd866fe24d80464`; patch SHA-256: `4e605e8295d237f13f3fc8393d7d90cf11d549a87b0940682da5f10e8869ce65`. These identify the source before the source and evidence were committed together; no future commit hash is inferred.

```bash
./gradlew :multiplatform:foundation:designsystem:jvmTest --rerun :multiplatform:data:trail:impl:jvmTest --rerun :multiplatform:feat:savetrail:impl:jvmTest --rerun :multiplatform:feat:filters:impl:jvmTest --rerun :multiplatform:screen:explore:impl:jvmTest --rerun :multiplatform:screen:traildetail:impl:jvmTest --rerun :multiplatform:screen:saved:impl:jvmTest --rerun :multiplatform:screen:navigate:impl:jvmTest --rerun :multiplatform:screen:activity:impl:jvmTest --rerun :multiplatform:screen:foryou:impl:jvmTest --rerun :multiplatform:di:graph:active:jvmTest --rerun :multiplatform:app:bootstrap:impl:jvmTest --rerun :multiplatform:app:core:jvmTest --rerun :multiplatform:app:core:compileKotlinJvm
```

BUILD SUCCESSFUL in 14s; 491 actionable tasks: 79 executed, 412 up-to-date. All 13 JVM test tasks and app-core JVM compilation executed freshly. **128 tests in 38 suites; zero failures/errors/skips.** No compiler warning lines in the final gate log. [Gate log](logs/roots-final-fix-jvm-gate.log); [counts](counts.json).

| Module | Suites | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `:multiplatform:foundation:designsystem` | 6 | 14 | 0 | 0 | 0 |
| `:multiplatform:data:trail:impl` | 10 | 36 | 0 | 0 | 0 |
| `:multiplatform:feat:savetrail:impl` | 5 | 22 | 0 | 0 | 0 |
| `:multiplatform:feat:filters:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:explore:impl` | 2 | 4 | 0 | 0 | 0 |
| `:multiplatform:screen:traildetail:impl` | 1 | 1 | 0 | 0 | 0 |
| `:multiplatform:screen:saved:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:screen:navigate:impl` | 2 | 5 | 0 | 0 | 0 |
| `:multiplatform:screen:activity:impl` | 4 | 11 | 0 | 0 | 0 |
| `:multiplatform:screen:foryou:impl` | 2 | 8 | 0 | 0 | 0 |
| `:multiplatform:di:graph:active` | 1 | 8 | 0 | 0 | 0 |
| `:multiplatform:app:bootstrap:impl` | 1 | 2 | 0 | 0 | 0 |
| `:multiplatform:app:core` | 2 | 14 | 0 | 0 | 0 |
| **Total** | **38** | **128** | **0** | **0** | **0** |

F-I1 now has inline shared sync-status mapping on Activity hero/rows and For You rows, including Offline waiting, retryable failure, syncing, finishing, paused, typed incompatible and removal on SYNCED. Heart tests retain save-only dispatch. F-I2 has Activity catalog readiness/error/offline projection, retained history and catalog payload on stream failure, Retry of both dependencies and observer reconnection. Presenter/UI cases preserve history/checkpoint state, expose Retry and recover the heart. F-M1 proves a new backend request for each same-account feed refresh after reopen and payload equality, including activity timestamps; F-M2 guarantees suspending cleanup before directory deletion; F-M3 covers all Navigate unavailable branches; F-M4 corrects Welcome attribution.

F-I3 remains pending the owner decision. No installed cached Activity Offline Retry assertion is established by this JVM package; master 8h remains unchecked. No installed-device work was performed by this implementer. Controller APK/device evidence is separate.

Development first failures are preserved in `logs/`; the initial sync-status and lifecycle failure XML is retained in `first-red-xml/`. The first sync tests failed because `Waiting for a connection` was absent. Two Activity recovery harness revisions supplied a lifecycle owner and Main dispatcher before the behavioral red reached the assertion that the pending catalog must keep root loading true. Those setup failures are not production-defect evidence. A Navigate test-only generic inference compilation error is separately retained; Navigate tests did not execute in that attempt. Initial recovery-test unnecessary non-null assertion warnings were corrected; they remain visible in historical development logs. No unchanged failing run was repeated for green.

The original 116-test gate, 119-test composite, navigation correction and their archived bytes remain unchanged. Deferred baseline compiler/tooling noise, F07 visual variance and expected drawer logger noise remain outside this wave. Prior clock, For You recovery/restoration, month totals, existing save dispatch and navigation-clipping closures remain unchanged.
