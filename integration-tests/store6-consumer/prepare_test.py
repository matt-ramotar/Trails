"""Synthetic preparation-guard regression tests; no Gradle or real library artifacts."""

import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("c3_prepare", Path(__file__).with_name("prepare.py"))
PREPARE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREPARE)


class PreparationGuardsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="trails-c3-guards-")
        self.root = Path(self.temporary.name)
        self.store = self.root / "store6"
        self.atom = self.root / "atom"
        self.repository = self.root / "repository"
        self.revision = "a" * 40
        self.version = "0.1.0-SNAPSHOT"
        for module in ("core", "compose"):
            path = self.atom / module / "build.gradle.kts"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("// synthetic source shape only\n")
        (self.atom / "gradle").mkdir()
        (self.atom / "gradle/libs.versions.toml").write_text(f'atom = "{self.version}"\n')
        message = self.root / "owner-message.txt"
        message.write_text("Synthetic owner message for preparation unit tests only")
        evidence = self.root / "owner-evidence.txt"
        evidence.write_text("Synthetic verification record; not a real dependency gate")
        self.handoff = self.root / "handoff.json"
        self.handoff.write_text(json.dumps({
            "status": "READY_FOR_C3_SOURCE_CONSUMPTION", "build_lease_released": True,
            "revision": self.revision, "version": self.version,
            "owner_thread_id": "synthetic-unit-test", "received_at_utc": "2026-09-15T00:00:00Z",
            "owner_message": {"path": str(message), "sha256": PREPARE.digest(message)},
            "evidence": [{"path": str(evidence), "sha256": PREPARE.digest(evidence)}],
        }))
        self.arguments = ["--store6-source", str(self.store), "--atom-source", str(self.atom),
                          "--atom-revision", self.revision, "--atom-version", self.version,
                          "--repository", str(self.repository), "--owner-handoff", str(self.handoff)]
        self.root_patch = patch.object(PREPARE, "ROOT", self.root)
        self.source_patch = patch.object(PREPARE, "check_source", side_effect=lambda path, revision: {
            "path": str(path), "revision": revision, "tree": "b" * 40, "clean": True})
        self.git_patch = patch.object(PREPARE.subprocess, "run")
        for active in (self.root_patch, self.source_patch, self.git_patch):
            active.start()
            self.addCleanup(active.stop)
        self.addCleanup(self.temporary.cleanup)

    def prepare_artifacts(self):
        for group, modules, version in (
            ("org/mobilenativefoundation/store", ("core", "sqldelight", "mutations", "mutations-sqldelight"), "6.0.0-SNAPSHOT"),
            ("dev/mattramotar/atom", ("core", "compose"), self.version),
        ):
            for module in modules:
                for suffix in ("", "-jvm", "-android"):
                    artifact = module + suffix
                    directory = self.repository / group / artifact / version
                    directory.mkdir(parents=True)
                    extensions = ["pom", "module", "aar" if suffix == "-android" else "jar"]
                    for extension in extensions:
                        (directory / f"{artifact}-{version}.{extension}").write_bytes(b"synthetic artifact bytes")
        with contextlib.redirect_stdout(io.StringIO()):
            PREPARE.main(self.arguments + ["--verify-artifacts"])
            PREPARE.check_candidate()

    def test_invalid_invocation_revokes_older_candidate(self):
        candidate = self.root / "candidate.properties"
        candidate.write_text("stale=true\n")
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            PREPARE.main([])
        self.assertFalse(candidate.exists())

    def test_source_only_preparation_revokes_older_candidate(self):
        self.prepare_artifacts()
        with contextlib.redirect_stdout(io.StringIO()):
            PREPARE.main(self.arguments)
        self.assertFalse((self.root / "candidate.properties").exists())

    def test_manifest_tampering_is_rejected(self):
        self.prepare_artifacts()
        manifest = self.root / "build/preparation/manifest.json"
        manifest.write_text(manifest.read_text() + "\n")
        with self.assertRaisesRegex(ValueError, "hash pairing"):
            PREPARE.check_candidate()

    def test_artifact_tampering_is_rejected(self):
        self.prepare_artifacts()
        next(self.repository.rglob("*.jar")).write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "artifact changed"):
            PREPARE.check_candidate()

    def test_missing_root_metadata_jar_revokes_older_candidate(self):
        self.prepare_artifacts()
        metadata = self.repository / "org/mobilenativefoundation/store/core/6.0.0-SNAPSHOT/core-6.0.0-SNAPSHOT.jar"
        metadata.unlink()
        with contextlib.redirect_stdout(io.StringIO()), self.assertRaisesRegex(ValueError, "Required consumer artifact is absent"):
            PREPARE.main(self.arguments + ["--verify-artifacts"])
        self.assertFalse((self.root / "candidate.properties").exists())

    def test_root_metadata_jar_tampering_is_rejected(self):
        self.prepare_artifacts()
        metadata = self.repository / f"dev/mattramotar/atom/compose/{self.version}/compose-{self.version}.jar"
        metadata.write_bytes(b"changed root metadata")
        with contextlib.redirect_stdout(io.StringIO()), self.assertRaisesRegex(ValueError, "artifact changed"):
            PREPARE.check_candidate()

    def test_owner_revision_mismatch_revokes_older_candidate(self):
        self.prepare_artifacts()
        handoff = json.loads(self.handoff.read_text())
        handoff["revision"] = "c" * 40
        self.handoff.write_text(json.dumps(handoff))
        with self.assertRaisesRegex(ValueError, "does not match"):
            PREPARE.main(self.arguments)
        self.assertFalse((self.root / "candidate.properties").exists())

    def test_owner_evidence_tampering_is_rejected(self):
        self.prepare_artifacts()
        (self.root / "owner-evidence.txt").write_text("changed")
        with self.assertRaisesRegex(ValueError, "evidence is absent or changed"):
            PREPARE.check_candidate()


if __name__ == "__main__":
    unittest.main()
