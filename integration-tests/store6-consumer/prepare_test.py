"""Synthetic preparation-guard regression tests; no Gradle or real library artifacts."""

import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("dependency_prepare", Path(__file__).with_name("prepare.py"))
PREPARE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREPARE)


class PreparationGuardsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="trails-dependency-guards-")
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

    def artifact_paths(self, trails_targets=False):
        targets = [("", "jar"), ("-jvm", "jar"), ("-android", "aar")]
        if trails_targets:
            targets += [("-iosarm64", "klib"), ("-iossimulatorarm64", "klib"),
                        ("-iosx64", "klib"), ("-js", "klib")]
        paths = []
        for group, modules, version in (
            ("org/mobilenativefoundation/store", ("core", "sqldelight", "mutations", "mutations-sqldelight"), "6.0.0-SNAPSHOT"),
            ("dev/mattramotar/atom", ("core", "compose"), self.version),
        ):
            for module in modules:
                for suffix, binary in targets:
                    artifact = module + suffix
                    directory = self.repository / group / artifact / version
                    paths += [directory / f"{artifact}-{version}.{extension}"
                              for extension in ("pom", "module", binary)]
        return paths

    def write_artifacts(self, trails_targets=False):
        for path in self.artifact_paths(trails_targets):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(f"Synthetic bytes for {path.relative_to(self.repository)}".encode())

    def invoke(self, arguments):
        with contextlib.redirect_stdout(io.StringIO()):
            try:
                PREPARE.main(arguments)
            except SystemExit as error:
                self.fail(f"CLI rejected supported arguments with exit code {error.code}: {arguments}")

    def prepare_artifacts(self, trails_targets=False):
        self.write_artifacts(trails_targets)
        flags = ["--verify-artifacts"] + (["--trails-targets"] if trails_targets else [])
        self.invoke(self.arguments + flags)
        with contextlib.redirect_stdout(io.StringIO()):
            PREPARE.check_candidate()

    def snapshot(self):
        return {str(path.relative_to(self.root)): (path.read_bytes(), path.stat().st_mtime_ns)
                for path in self.root.rglob("*") if path.is_file()}

    def test_default_candidate_retains_jvm_android_fixture_scope(self):
        self.prepare_artifacts()
        record = json.loads((self.root / "build/preparation/manifest.json").read_text())
        self.assertEqual(len(record["artifact_files"]), 54)
        self.assertEqual({entry["path"] for entry in record["artifact_files"]},
                         {str(path.relative_to(self.repository)) for path in self.artifact_paths()})
        self.invoke(["--check-candidate"])

    def test_trails_candidate_records_all_required_publications_and_hashes(self):
        self.prepare_artifacts(trails_targets=True)
        record = json.loads((self.root / "build/preparation/manifest.json").read_text())
        self.assertEqual(len(record["artifact_files"]), 126)
        self.assertEqual({entry["path"]: entry["sha256"] for entry in record["artifact_files"]},
                         {str(path.relative_to(self.repository)): PREPARE.digest(path)
                          for path in self.artifact_paths(trails_targets=True)})
        before = self.snapshot()
        self.invoke(["--check-candidate", "--trails-targets"])
        self.invoke(["--trails-targets", "--check-candidate"])
        self.assertEqual(self.snapshot(), before)

    def test_missing_native_or_js_publication_file_revokes_older_candidate(self):
        for target in ("iosarm64", "iossimulatorarm64", "iosx64", "js"):
            for extension in ("pom", "module", "klib"):
                with self.subTest(target=target, extension=extension):
                    self.prepare_artifacts()
                    self.write_artifacts(trails_targets=True)
                    missing = self.repository / f"dev/mattramotar/atom/core-{target}/{self.version}/core-{target}-{self.version}.{extension}"
                    missing.unlink()
                    with self.assertRaisesRegex(ValueError, "Required consumer artifact is absent"):
                        self.invoke(self.arguments + ["--verify-artifacts", "--trails-targets"])
                    self.assertFalse((self.root / "candidate.properties").exists())

    def test_trails_check_rejects_fixture_manifest_even_when_all_files_exist(self):
        self.prepare_artifacts()
        self.write_artifacts(trails_targets=True)
        before = self.snapshot()
        with self.assertRaisesRegex(ValueError, "Required Trails artifact was not recorded"):
            self.invoke(["--check-candidate", "--trails-targets"])
        self.assertEqual(self.snapshot(), before)

    def test_trails_check_rejects_missing_recorded_native_file_without_mutation(self):
        self.prepare_artifacts(trails_targets=True)
        native = self.repository / f"dev/mattramotar/atom/core-iosarm64/{self.version}/core-iosarm64-{self.version}.klib"
        native.unlink()
        before = self.snapshot()
        with self.assertRaises((ValueError, FileNotFoundError)):
            self.invoke(["--check-candidate", "--trails-targets"])
        self.assertEqual(self.snapshot(), before)

    def test_trails_check_rejects_native_and_js_tampering_without_mutation(self):
        for target in ("iosarm64", "js"):
            with self.subTest(target=target):
                self.prepare_artifacts(trails_targets=True)
                binary = self.repository / f"dev/mattramotar/atom/core-{target}/{self.version}/core-{target}-{self.version}.klib"
                binary.write_bytes(b"tampered native or JS binary")
                before = self.snapshot()
                with self.assertRaisesRegex(ValueError, "artifact changed"):
                    self.invoke(["--check-candidate", "--trails-targets"])
                self.assertEqual(self.snapshot(), before)

    def test_check_mode_rejects_preparation_arguments_without_mutation(self):
        self.prepare_artifacts()
        before = self.snapshot()
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as error:
            PREPARE.main(["--check-candidate", "--trails-targets", "--verify-artifacts"])
        self.assertEqual(error.exception.code, 2)
        self.assertEqual(self.snapshot(), before)

    def test_help_preserves_candidate(self):
        self.prepare_artifacts()
        before = self.snapshot()
        for arguments in (["--help"], ["--check-candidate", "--help"]):
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(SystemExit) as error:
                PREPARE.main(arguments)
            self.assertEqual(error.exception.code, 0)
        self.assertEqual(self.snapshot(), before)

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
