# Standalone C3 consumer — not yet compiled

This fixture is prepared for Trails' joint Store6/repaired Atom gate. **It has not passed C3.** It cannot configure until `prepare.py` verifies a clean Atom Task4 handoff and the selected isolated JVM/Android artifact files. Atom implementation `05daa800ac3c6d0dc1f9538234b99061c8139e40` is now known and its clean source APIs match this fixture; the owner's final evidence and released build lease are still pending. A dirty checkout is not accepted.

See [dependency authority and constraints](../../docs/store6-integration.md) and the [execution recipe](../../docs/evidence/m1/dependencies/REPRODUCTION.md).

The source exercises generated SQLDelight rows, a dedicated persistent mutation journal, fake-server Offline state/receipts across reopening, a finite manual Atom enqueue command, and Compose/Circuit/Metro consumption. Five JVM tests are written but unexecuted. The Android host registers one parcelable `FixtureScreen` with `Circuit.Builder`, using the Metro-provided presenter/UI, and renders it through `CircuitContent`. Its diagnostic package, databases and control surface are separate from the Trails product design and M1 acceptance.

The provisional consumer tuple is Kotlin/Compose compiler 2.3.20, Compose 1.9.1, Metro 0.11.3, KSP 2.3.10, Circuit 0.30.0, SQLDelight 2.1.0, AGP 8.12.3 and Gradle 8.13. Atom's full SHA/version are required inputs. Atom construction is manual; the KSP plugin is present without the Atom processor or generated factory registry.

`prepare.py` requires `--owner-handoff`: the local coordinator's record of the received owner message, exact revision/version, released build lease and hashed evidence. The example starts in a blocked state. This checks record consistency and bytes; the coordinator still verifies who approved it and what the evidence proves. A clean descendant with the right folders is insufficient.

Every preparation attempt revokes previous candidate properties before validation; source-only preparation cannot enable a build. Settings checks the paired manifest, handoff and artifact hashes, and `verifyCandidate` repeats verification before compile tasks. Configuration caching is disabled for this fixture so these checks are not bypassed by a reused configuration.

`prepare.py` does not clone sources, invoke Gradle, publish artifacts, or assert behavioral verification. Its artifact hashes must be paired with the actual producer logs. Eight synthetic Python guard tests pass; they use fake artifact bytes and do not validate the Kotlin libraries. The main Trails build has not been changed to include this fixture.

Root KMP metadata JARs are required and hashed alongside root POM/module metadata and JVM/Android binaries. The missing-file and changed-byte regressions failed before the fix; [red and green evidence](../../docs/evidence/m1/dependencies/metadata-jar-guard/) preserves those results. Reprepare any older candidate so its manifest includes the root metadata JARs.
