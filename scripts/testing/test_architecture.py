"""Boundary checks for the lightweight Gradle/source architecture verifier."""

from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_architecture


class ArchitectureTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)

    def modules(self, *names):
        self.write('settings.gradle.kts', '\n'.join(f'include("{name}")' for name in names))
        for name in names:
            self.write(name.strip(':').replace(':', '/') + '/build.gradle.kts', '')

    def test_nested_and_compact_tests_do_not_become_production_edges(self):
        dependencies = list(check_architecture.project_dependencies('''
            // implementation(projects.ignored.comment)
            commonMain { dependencies { api(projects.example.api) } }
            commonTest.dependencies { implementation(projects.example.impl) }
            jvmTest { dependencies { implementation(project(":example:impl")) } }
            dependencies { testImplementation(project(":example:impl")) }
            val message = "a brace } is not a scope"
            androidMain.dependencies { implementation(project(":example:api")) }
        '''))
        self.assertEqual([(name, production) for name, production, _ in dependencies], [
            (':example:api', True), (':example:impl', False), (':example:impl', False),
            (':example:impl', False), (':example:api', True),
        ])

    def test_reports_production_cycle_but_allows_test_only_reverse_edge(self):
        self.modules(':multiplatform:screen:first:api', ':multiplatform:screen:second:api')
        first = 'multiplatform/screen/first/api/build.gradle.kts'
        second = 'multiplatform/screen/second/api/build.gradle.kts'
        self.write(first, 'commonMain.dependencies { api(projects.multiplatform.screen.second.api) }')
        self.write(second, 'commonTest.dependencies { api(projects.multiplatform.screen.first.api) }')
        self.assertEqual(check_architecture.check(self.root)[0], [])
        self.write(second, 'commonMain.dependencies { api(projects.multiplatform.screen.first.api) }')
        self.assertTrue(any('dependency cycle' in error for error in check_architecture.check(self.root)[0]))

    def test_only_composition_roots_consume_implementations_in_production(self):
        self.modules(':multiplatform:screen:first:impl', ':multiplatform:screen:second:impl', ':multiplatform:app:runtime')
        path = 'multiplatform/screen/first/impl/build.gradle.kts'
        dependency = 'implementation(projects.multiplatform.screen.second.impl)'
        self.write(path, 'jvmTest.dependencies { ' + dependency + ' }')
        self.write('multiplatform/app/runtime/build.gradle.kts', 'commonMain.dependencies { ' + dependency + ' }')
        self.assertEqual(check_architecture.check(self.root)[0], [])
        self.write(path, 'commonMain.dependencies { ' + dependency + ' }')
        self.assertTrue(any('composed only' in error for error in check_architecture.check(self.root)[0]))

    def test_reports_inactive_modules_and_missing_builds(self):
        self.modules(':apps:android', ':multiplatform:missing')
        (self.root / 'multiplatform/missing/build.gradle.kts').unlink()
        self.write('apps/android/build.gradle.kts', 'implementation(projects.multiplatform.retired)')
        self.write('multiplatform/orphan/build.gradle.kts', '')
        errors, _ = check_architecture.check(self.root)
        self.assertTrue(any('not included' in error for error in errors))
        self.assertTrue(any('has no build.gradle.kts' in error for error in errors))
        self.assertTrue(any('inactive module' in error for error in errors))

    def test_checks_packages_and_retired_imports_while_ignoring_generated_code(self):
        self.modules(':apps:android')
        self.write('apps/android/src/main/kotlin/example/Screen.kt',
                   'package wrong\nimport org.mobilenativefoundation.trails.domain.User\n')
        self.write('apps/android/build/generated/src/main/kotlin/example/Generated.kt', 'package wrong')
        errors, _ = check_architecture.check(self.root)
        self.assertEqual(len(errors), 2)
        self.assertTrue(any('package must match' in error for error in errors))
        self.assertTrue(any('retired production import' in error for error in errors))

    def test_data_module_cannot_acquire_ui_through_convention_plugin(self):
        self.modules(':multiplatform:data:example')
        self.write('tooling/plugins/build.gradle.kts', '''
            register("ui") { id = "plugin.trails.compose"
                implementationClass = "example.ComposePlugin" }
        ''')
        self.write('tooling/plugins/src/main/kotlin/example/ComposePlugin.kt', '''
            package example
            fun dependencies() { implementation("org.jetbrains.compose.runtime:runtime:1") }
        ''')
        self.write('multiplatform/data/example/build.gradle.kts', 'plugins { id("plugin.trails.compose") }')
        errors, _ = check_architecture.check(self.root)
        self.assertTrue(any('must apply plugin.trails.library' in error for error in errors))
        self.assertTrue(any('UI dependency or plugin' in error for error in errors))


if __name__ == '__main__':
    unittest.main()
