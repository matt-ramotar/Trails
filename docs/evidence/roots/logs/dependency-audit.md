# Candidate source recovery diagnosis

Date: 2026-09-20. Scope: read-only dependency diagnosis, plus this report. No Gradle, publication, dependency update, Git mutation, cache/daemon probe, or rerun of the failing candidate check was performed.

## Finding

The recorded Store6 source is partially deleted. It is not a dangling Git worktree link. `/private/tmp/trails-c3-20260915/store6/.git` is a directory, but `HEAD`, `config`, and `packed-refs` are absent, and its remaining `refs` and `logs` directories contain no files. Git therefore cannot recognize this directory as a repository. The controller's first failure from `prepare.py --check-candidate` is consistent with this state.

The damage also affects source files. A read-only comparison against the exact pinned commit in `/Users/matt/src/matt-ramotar/Store6` found 815 tracked blobs: 770 absent, 45 present and byte-identical by Git blob SHA-1, zero changed present blobs. Missing files include `.gitignore`, `AGENTS.md`, `README.md`, build scripts, workflows, and Kotlin sources. Restoring only `.git/HEAD` would leave a severely incomplete checkout and is not an adequate repair. The deletion cause is unknown; the observed dates alone do not prove a cleanup process or actor.

## Current identity and integrity evidence

| Item | Observed result |
|---|---|
| Trails branch | `matt-ramotar/remaining-roots` |
| Candidate Store6 revision | `582edfe86e64ddc71312ecd20a1895fc3de37b52` |
| Recorded Store6 tree | `554d0c82ed945298f12497ef722b28de2c190aa4` |
| Canonical local Store6 object | Commit exists; its tree matches the recorded tree |
| Candidate Atom revision | `05daa800ac3c6d0dc1f9538234b99061c8139e40` |
| Recorded and live Atom tree | `12591079051f68d0f3d6361347c968dbbf2d2124` |
| Atom source | Recorded path is a valid repository; live HEAD matches; porcelain status including ordinary untracked files is empty |
| Atom version | `0.1.0-SNAPSHOT` |
| Isolated artifact repository | `/private/tmp/trails-c3-20260915/maven` |
| Preparation manifest SHA-256 | `5f6a07d6ee7e16e87a0b48072c8286c71e1d1ed1fd09b299b622b0a5be341cf7`, matches candidate properties |
| Artifact files | All 54 recorded files exist and match their SHA-256 values |
| Owner handoff and referenced evidence | Handoff plus four references exist and match recorded SHA-256 values |

The manifest is `integration-tests/store6-consumer/build/preparation/manifest.json`; the pairing is held by `integration-tests/store6-consumer/candidate.properties`. These files were read and not changed. No Atom repair is indicated. Existing untracked Trails evidence was not modified.

## Minimal reversible recovery

Controller authorization states that reversible restoration at the pinned revision is within the original task. Run each command separately. The recovery destination and backup must not already exist; stop rather than replace an existing path. No checkout or reset of the canonical Store6 working tree is needed.

1. Create a standalone local clone with independent object files. A local clone copies repository objects, not the canonical worktree's uncommitted files.

```bash
git clone --no-hardlinks --no-checkout /Users/matt/src/matt-ramotar/Store6 /private/tmp/trails-c3-20260915/store6-recovered-20260920
```

2. Check out only the pinned commit.

```bash
git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 checkout --detach 582edfe86e64ddc71312ecd20a1895fc3de37b52
```

3. Verify the new clone before moving the damaged source. The first command must return the exact recorded commit and tree; the second must return no output.

```bash
git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 rev-parse HEAD 'HEAD^{tree}'
```

```bash
git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 status --porcelain --untracked-files=normal
```

4. After those checks pass, preserve the entire damaged directory, then put the verified clone at the unchanged recorded path. `mv -n` avoids overwriting an existing file; the controller must also confirm neither destination already exists, since a directory destination could cause nesting.

```bash
mv -n /private/tmp/trails-c3-20260915/store6 /private/tmp/trails-c3-20260915/store6-damaged-20260920
```

```bash
mv -n /private/tmp/trails-c3-20260915/store6-recovered-20260920 /private/tmp/trails-c3-20260915/store6
```

The first rename preserves all remaining source, Git metadata, ignored build output, and any untracked files, not just the 45 surviving tracked blobs. If the second rename fails, restore the damaged directory to its original name before proceeding. Never merge directories, delete the damaged directory, overwrite the candidate manifest, replace artifact bytes, or regenerate candidate properties as part of this recovery.

5. Run the unchanged candidate check once after the source has materially changed through restoration. Preserve its new result separately from the first failure.

```bash
python3 integration-tests/store6-consumer/prepare.py --check-candidate
```

If it passes, its statement is only `CANDIDATE_HASHES_MATCH_CONSUMER_STILL_REQUIRES_EXECUTION`. Gradle validation and the plan's required runtime checks remain separate work for the designated runner.

## Limits

The canonical repository contains the required commit and matches the recorded tree, but a complete clone has not been attempted by this audit. Clone/checkout failure is a new stop condition to investigate without changing pins. The source-content comparison enumerated tracked blobs; it did not classify every ignored or untracked file, which is why the entire damaged directory must be preserved. No metadata provenance is fabricated: the original damaged metadata remains in the backup, while the new clone transparently records recovery from the local canonical repository.

The artifact and handoff checks here are diagnostics, not a replacement for `prepare.py`. No consumer compilation, tests, Android installation, or acceptance checks ran in this subtask.

Memory lookup used `MEMORY.md:172` only to confirm the historical M1 dependency tuple; the tuple and integrity findings above were verified from current files and commands. Related rollout ID: `01a0a5a8-87f5-7563-bb6a-2554d87819be`.

## Authorized recovery execution

After the initial diagnosis, the controller explicitly authorized the documented reversible repair. All following commands completed with exit code 0, sequentially except the independent HEAD/tree and status reads. No Gradle task ran.

1. A Python preflight confirmed both recovery and damaged-backup paths were absent and recorded candidate properties SHA-256 `ead03beeb7b5c1352c350fd55ed25506e219c478d30a39d7d8d78423a154cc3a` and manifest SHA-256 `5f6a07d6ee7e16e87a0b48072c8286c71e1d1ed1fd09b299b622b0a5be341cf7`.
2. `git clone --no-hardlinks --no-checkout /Users/matt/src/matt-ramotar/Store6 /private/tmp/trails-c3-20260915/store6-recovered-20260920` succeeded.
3. `git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 checkout --detach 582edfe86e64ddc71312ecd20a1895fc3de37b52` succeeded.
4. `git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 rev-parse HEAD 'HEAD^{tree}'` returned exactly `582edfe86e64ddc71312ecd20a1895fc3de37b52` and `554d0c82ed945298f12497ef722b28de2c190aa4`. `git -C /private/tmp/trails-c3-20260915/store6-recovered-20260920 status --porcelain --untracked-files=normal` returned no output.
5. `mv -n /private/tmp/trails-c3-20260915/store6 /private/tmp/trails-c3-20260915/store6-damaged-20260920` preserved the full damaged directory.
6. `mv -n /private/tmp/trails-c3-20260915/store6-recovered-20260920 /private/tmp/trails-c3-20260915/store6` placed the verified clone at the unchanged manifest path.
7. `python3 integration-tests/store6-consumer/prepare.py --check-candidate > .superpowers/sdd/dependency-candidate-restored-20260920.log 2>&1` ran exactly once after restoration and exited 0. Its retained output is `CANDIDATE_HASHES_MATCH_CONSUMER_STILL_REQUIRES_EXECUTION`.
8. Final read-only checks confirmed both recorded candidate/manifest hashes remain unchanged, the damaged backup and its original `.git/index` exist, and restored `.git/HEAD` contains the pinned Store6 SHA.

Recovery is complete. Atom, isolated artifacts, candidate properties, manifest, and owner evidence were not edited. The original first failure remains distinct from this post-recovery result. Consumer compilation and tests remain the controller's work under its build lease.
