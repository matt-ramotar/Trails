"""Regression fixtures for the production Kotlin component boundary."""

import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'check_components.py'
SOURCE = 'multiplatform/screen/example/impl/src/commonMain/kotlin/example/Example.kt'
SHELL = ('multiplatform/app/runtime/src/commonMain/kotlin/org/'
         'mobilenativefoundation/trails/app/runtime/AccountContent.kt')


class ComponentCheckTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(SCRIPT.is_file(), 'Component source checker is missing')
        spec = importlib.util.spec_from_file_location('check_components', SCRIPT)
        self.checker = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.checker)

    def check(self, source, path=SOURCE):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            file = root / path
            file.parent.mkdir(parents=True)
            file.write_text(source)
            return self.checker.check(root)

    def assert_violation(self, source, symbol, line=1, path=SOURCE):
        errors, count = self.check(source, path)
        self.assertEqual(1, count)
        self.assertEqual(1, len(errors), errors)
        self.assertIn(f'{path}:{line}:', errors[0])
        self.assertIn(symbol, errors[0])

    def test_rejects_direct_visual_control_import_even_when_unused(self):
        self.assert_violation('import androidx.compose.material3.Slider\n', 'Slider')

    def test_rejects_alias_and_backtick_alias_imports(self):
        for alias in ('NativeSlider', '`native slider`'):
            with self.subTest(alias=alias):
                self.assert_violation(f'import androidx.compose.material3.Slider as {alias}\n'
                                      f'fun Ui() {{ {alias}(value = 0f) }}', 'Slider')

    def test_rejects_wildcard_control_call_at_call_line(self):
        self.assert_violation('import androidx.compose.material3.*\n\n'
                              'fun Ui() { Slider(value = 0f) }', 'Slider', line=3)

    def test_rejects_fully_qualified_control_without_import(self):
        self.assert_violation('fun Ui() { androidx.compose.material3.Slider(value = 0f) }', 'Slider')

    def test_rejects_spaced_and_multiline_fully_qualified_control(self):
        self.assert_violation('fun Ui() { androidx . compose .\n material3 . Button({}) {} }',
                              'Button', line=1)

    def test_rejects_callable_reference_and_defaults_from_wildcard(self):
        for expression, symbol in (('::Button', 'Button'), ('ButtonDefaults.buttonColors()', 'ButtonDefaults')):
            with self.subTest(expression=expression):
                self.assert_violation('import androidx.compose.material3.*\n'
                                      f'val example = {expression}', symbol, line=2)

    def test_rejects_material_legacy_controls(self):
        self.assert_violation('import androidx.compose.material.CircularProgressIndicator\n',
                              'CircularProgressIndicator')

    def test_rejects_imports_of_visual_defaults_members(self):
        for suffix in ('buttonColors as nativeColors', '*', 'buttonColors'):
            with self.subTest(suffix=suffix):
                self.assert_violation('import androidx.compose.material3.ButtonDefaults.' + suffix,
                                      'ButtonDefaults')

    def test_rejects_control_in_nested_material_package(self):
        self.assert_violation('import androidx.compose.material3.pulltorefresh.PullToRefreshBox\n',
                              'PullToRefreshBox')

    def test_allows_text_icon_native_state_and_annotations(self):
        errors, count = self.check('''import androidx.compose.material3.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
fun Ui(state: DrawerState) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Text("Slider")
    Icon(icon, contentDescription = null)
}
''')
        self.assertEqual([], errors)
        self.assertEqual(1, count)

    def test_ignores_comments_strings_and_character_literals(self):
        errors, _ = self.check('''import androidx.compose.material3.*
// Slider(value = 0f)
/* Button() /* Nested: Switch() */ TextButton() */
val sample = "androidx.compose.material3.Slider()"
val quote = '\\''
val raw = """Button() // not code
import androidx.compose.material3.Slider
"""
val escaped = "quote: \\\"Button()\\\""
''')
        self.assertEqual([], errors)

    def test_keeps_line_numbers_after_multiline_comments(self):
        self.assert_violation('/* first\n second */\n'
                              'import androidx.compose.material3.Button\n', 'Button', line=3)

    def test_unrelated_local_names_without_material_import_are_allowed(self):
        errors, _ = self.check('fun Button() {}\nfun Ui() { Button(); model.Card() }')
        self.assertEqual([], errors)

    def test_local_declaration_takes_precedence_over_wildcard(self):
        errors, _ = self.check('import androidx.compose.material3.*\n'
                               'fun Button() {}\nfun Ui() { Button(); model.Card() }')
        self.assertEqual([], errors)

    def test_explicit_other_import_takes_precedence_over_wildcard(self):
        errors, _ = self.check('import androidx.compose.material3.*\n'
                               'import example.Card\nimport example.Action as Button\n'
                               'fun Ui() { Card(); Button() }')
        self.assertEqual([], errors)

    def test_function_parameter_does_not_become_material_control(self):
        errors, _ = self.check('import androidx.compose.material3.*\n'
                               'fun Ui(Button: () -> Unit) { Button() }')
        self.assertEqual([], errors)

    def test_local_shadow_does_not_hide_material_call_in_another_scope(self):
        self.assert_violation('import androidx.compose.material3.*\n'
                              'fun Ui() { val Button = {}; Button() }\n'
                              'fun Other() { Button({}) {} }', 'Button', line=3)

    def test_allows_scaffold_only_in_exact_account_shell(self):
        source = 'import androidx.compose.material3.*\nfun Ui() { Scaffold {} }'
        errors, _ = self.check(source, SHELL)
        self.assertEqual([], errors)
        self.assert_violation(source, 'Scaffold', line=2)
        self.assert_violation(source, 'Scaffold', line=2,
                              path=SOURCE.replace('Example.kt', 'AccountContent.kt'))

    def test_scaffold_exception_does_not_allow_other_controls(self):
        self.assert_violation('import androidx.compose.material3.Button\n', 'Button', path=SHELL)

    def test_design_system_owns_native_visual_implementations(self):
        errors, count = self.check('import androidx.compose.material3.Slider\n',
                                   'multiplatform/foundation/designsystem/src/commonMain/kotlin/example/Slider.kt')
        self.assertEqual([], errors)
        self.assertEqual(0, count)

    def test_test_and_generated_build_sources_are_excluded(self):
        for path in ('multiplatform/screen/example/impl/src/jvmTest/kotlin/example/Test.kt',
                     'multiplatform/screen/example/impl/build/generated/src/commonMain/kotlin/example/Ui.kt'):
            with self.subTest(path=path):
                errors, count = self.check('import androidx.compose.material3.Slider\n', path)
                self.assertEqual([], errors)
                self.assertEqual(0, count)

    def test_android_production_sources_are_checked(self):
        self.assert_violation('import androidx.compose.material3.Button\n', 'Button',
                              path='apps/android/src/main/kotlin/example/MainActivity.kt')

    def test_command_exit_code_and_summary(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / SOURCE
            path.parent.mkdir(parents=True)
            path.write_text('import androidx.compose.material3.Button\n')
            result = subprocess.run([sys.executable, str(SCRIPT), '--root', directory],
                                    capture_output=True, text=True)
            self.assertEqual(1, result.returncode)
            self.assertIn('Component check: 1 sources, 1 violations', result.stdout)
            self.assertIn('Button', result.stderr)


if __name__ == '__main__':
    unittest.main()
