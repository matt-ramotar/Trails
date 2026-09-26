#!/usr/bin/env python3
"""Verify dependency source revisions and record the isolated dependency artifacts.

Does not clone, publish, invoke Gradle, or accept the dirty application/library checkout.
Retain publisher logs and run consumer tests separately.
"""

import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

STORE_REVISION = "582edfe86e64ddc71312ecd20a1895fc3de37b52"
ATOM_REPAIR = "be581804076bbfc18d8fdd3edf5ddcf8844cd909"
ROOT = Path(__file__).resolve().parent


def git(path, *arguments):
    return subprocess.check_output(["git", "-C", str(path), *arguments], text=True).strip()


def check_source(path, revision):
    path = path.resolve(strict=True)
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Supply a full immutable revision, not a branch or tag.")
    if git(path, "rev-parse", "HEAD") != revision:
        raise ValueError(f"{path} is not at required revision {revision}")
    dirty = git(path, "status", "--porcelain", "--untracked-files=normal")
    if dirty:
        raise ValueError(f"{path} is dirty; prepare a clean isolated checkout of the handoff commit.")
    return {"path": str(path), "revision": revision, "tree": git(path, "rev-parse", "HEAD^{tree}"), "clean": True}


def artifact_paths(group, artifact, version, trails_targets=False):
    # The standalone fixture consumes JVM/Android; the app also resolves iOS and JS.
    publications = [("", "jar"), ("-jvm", "jar"), ("-android", "aar")]
    if trails_targets:
        publications += [("-iosarm64", "klib"), ("-iossimulatorarm64", "klib"),
                         ("-iosx64", "klib"), ("-js", "klib")]
    for suffix, binary in publications:
        module = artifact + suffix
        directory = Path(group.replace(".", "/")) / module / version
        # The root KMP publication carries common metadata in its own JAR.
        for extension in ("pom", "module", binary):
            yield directory / f"{module}-{version}.{extension}"


def consumer_modules(atom_version):
    for module in ("core", "sqldelight", "mutations", "mutations-sqldelight"):
        yield "org.mobilenativefoundation.store", module, "6.0.0-SNAPSHOT"
    for module in ("core", "compose"):
        yield "dev.mattramotar.atom", module, atom_version


def artifact_files(repository, group, artifact, version, trails_targets=False):
    results = []
    for relative in artifact_paths(group, artifact, version, trails_targets):
        file = repository / relative
        if not file.is_file():
            raise ValueError(f"Required consumer artifact is absent: {file}")
        if repository not in file.resolve().parents:
            raise ValueError(f"Artifact resolves outside the isolated repository: {file}")
        results.append({"path": str(relative), "sha256": digest(file)})
    return results


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_recorded_handoff(path, revision, version):
    path = path.resolve(strict=True)
    record = json.loads(path.read_text())
    if record.get("status") != "READY_FOR_C3_SOURCE_CONSUMPTION" or record.get("build_lease_released") is not True:
        raise ValueError("The recorded dependency handoff must include source approval and a released build lease.")
    if record.get("revision") != revision or record.get("version") != version:
        raise ValueError("The recorded owner handoff does not match the candidate revision/version.")
    if not record.get("owner_thread_id") or not record.get("received_at_utc"):
        raise ValueError("Record the owner thread and received message time.")
    evidence = record.get("evidence", [])
    if not evidence:
        raise ValueError("The handoff must reference the owner's verification evidence.")
    referenced = []
    for item in [record.get("owner_message", {})] + evidence:
        reference = Path(item.get("path", ""))
        if not reference.is_absolute():
            reference = path.parent / reference
        reference = reference.resolve(strict=True)
        if not reference.is_file() or digest(reference) != item.get("sha256"):
            raise ValueError(f"Owner handoff evidence is absent or changed: {reference}")
        referenced.append({"path": str(reference), "sha256": item["sha256"]})
    # This validates the recorded handoff and its bytes, not authorship or test quality.
    return {"path": str(path), "sha256": digest(path), "referenced_files": referenced,
            "guarantee": "Recorded handoff consistency only; source approval and evidence review require human verification."}


def check_candidate(trails_targets=False):
    candidate_path = ROOT / "candidate.properties"
    properties = dict(line.split("=", 1) for line in candidate_path.read_text().splitlines() if line)
    manifest_path = Path(properties["manifestPath"]).resolve(strict=True)
    if digest(manifest_path) != properties["manifestSha256"]:
        raise ValueError("Candidate/manifest hash pairing changed. Prepare the candidate again.")
    record = json.loads(manifest_path.read_text())
    if record["status"] != "SOURCE_AND_ARTIFACT_FILES_VERIFIED_CONSUMER_UNEXECUTED":
        raise ValueError("Source-only preparation is not eligible for compilation.")
    expected = {"isolatedMavenRepository": record["isolated_repository"], "atomVersion": record["atom_version"],
                "atomRevision": record["sources"]["atom"]["revision"], "store6Revision": STORE_REVISION}
    if any(properties.get(key) != value for key, value in expected.items()):
        raise ValueError("Candidate properties disagree with the recorded manifest.")
    for source in record["sources"].values():
        if check_source(Path(source["path"]), source["revision"])["tree"] != source["tree"]:
            raise ValueError("A prepared source tree changed.")
    handoff = record["owner_handoff"]
    if digest(Path(handoff["path"])) != handoff["sha256"]:
        raise ValueError("The owner handoff record changed.")
    verify_recorded_handoff(Path(handoff["path"]), expected["atomRevision"], expected["atomVersion"])
    repository = Path(record["isolated_repository"]).resolve(strict=True)
    if not record["artifact_files"]:
        raise ValueError("No artifact bytes were recorded.")
    if trails_targets:
        recorded_paths = {artifact["path"] for artifact in record["artifact_files"]}
        for group, module, version in consumer_modules(record["atom_version"]):
            for required in artifact_paths(group, module, version, trails_targets=True):
                if str(required) not in recorded_paths:
                    raise ValueError(f"Required Trails artifact was not recorded: {required}. Prepare again with --trails-targets.")
    for artifact in record["artifact_files"]:
        path = (repository / artifact["path"]).resolve(strict=True)
        if repository not in path.parents or digest(path) != artifact["sha256"]:
            raise ValueError(f"A candidate artifact changed: {path}")
    print("CANDIDATE_HASHES_MATCH_CONSUMER_STILL_REQUIRES_EXECUTION")


def main(argv=None):
    arguments = sys.argv[1:] if argv is None else argv
    parser = argparse.ArgumentParser(description=__doc__, allow_abbrev=False)
    parser.add_argument("--check-candidate", action="store_true", help="Verify the recorded candidate without changing it.")
    parser.add_argument("--trails-targets", action="store_true", help="Require iOS and JS publications in addition to the JVM/Android fixture.")
    if "--check-candidate" in arguments:
        # Parse only verification flags so invalid checks cannot revoke or rewrite a candidate.
        args = parser.parse_args(arguments)
        check_candidate(trails_targets=args.trails_targets)
        return
    # An invalid invocation or a source-only recheck must not leave an older candidate usable.
    if "--help" not in arguments and "-h" not in arguments:
        (ROOT / "candidate.properties").unlink(missing_ok=True)
    parser.add_argument("--store6-source", required=True, type=Path)
    parser.add_argument("--atom-source", required=True, type=Path)
    parser.add_argument("--atom-revision", required=True)
    parser.add_argument("--atom-version", required=True)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--verify-artifacts", action="store_true")
    parser.add_argument("--owner-handoff", required=True, type=Path)
    args = parser.parse_args(arguments)
    handoff = verify_recorded_handoff(args.owner_handoff, args.atom_revision, args.atom_version)
    sources = {"store6": check_source(args.store6_source, STORE_REVISION), "atom": check_source(args.atom_source, args.atom_revision)}
    subprocess.run(["git", "-C", str(args.atom_source), "merge-base", "--is-ancestor", ATOM_REPAIR, args.atom_revision], check=True)
    for module in ("core", "compose"):
        if not (args.atom_source / module / "build.gradle.kts").is_file():
            raise ValueError(f"Atom handoff does not contain the required optional {module} module.")
    versions = (args.atom_source / "gradle/libs.versions.toml").read_text()
    match = re.search(r'^atom\s*=\s*"([^"]+)"', versions, flags=re.MULTILINE)
    if not match or match.group(1) != args.atom_version:
        raise ValueError("Atom version does not match the clean handoff source catalog.")
    repository = args.repository.resolve()
    ambient = (Path.home() / ".m2/repository").resolve()
    if repository == ambient or ambient in repository.parents:
        raise ValueError("Use a separate artifact repository outside ambient Maven Local.")
    for text in (str(repository), args.atom_version):
        if any(character in text for character in ("\n", "\r", "\\")):
            raise ValueError("Candidate property values must contain no newline or escape characters.")
    record = {"recorded_utc": datetime.now(timezone.utc).isoformat(), "status": "SOURCES_VERIFIED_ONLY", "sources": sources,
              "isolated_repository": str(repository), "atom_version": args.atom_version, "owner_handoff": handoff, "artifact_files": [], "gradle_executed_by_this_script": False}
    if args.verify_artifacts:
        for group, module, version in consumer_modules(args.atom_version):
            record["artifact_files"] += artifact_files(repository, group, module, version, args.trails_targets)
        record["status"] = "SOURCE_AND_ARTIFACT_FILES_VERIFIED_CONSUMER_UNEXECUTED"
    output = ROOT / "build" / "preparation"
    output.mkdir(parents=True, exist_ok=True)
    manifest = output / "manifest.json"
    manifest.write_text(json.dumps(record, indent=2) + "\n")
    if args.verify_artifacts:
        candidate = output / "candidate.properties.pending"
        candidate.write_text(
            f"isolatedMavenRepository={repository}\natomVersion={args.atom_version}\natomRevision={args.atom_revision}\nstore6Revision={STORE_REVISION}\n"
            f"manifestPath={manifest.resolve()}\nmanifestSha256={digest(manifest)}\n"
        )
        candidate.replace(ROOT / "candidate.properties")
    print(record["status"])
    print(output / "manifest.json")


if __name__ == "__main__":
    main()
