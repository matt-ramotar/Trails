#!/usr/bin/env python3
"""Check Trails' declared module graph and authored Kotlin source without running Gradle.

This reads literal include/project declarations and the checked-in convention plugins.
Keep declarations literal or extend this check when introducing a different Gradle DSL form.
"""

import argparse
import os
from pathlib import Path
import re
import sys

IGNORED = {'build', '.gradle', '.git', '.kotlin', '__pycache__'}
COMMENTS = re.compile(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*.*?\*/', re.S)
PROJECT = re.compile(r'\bprojects\.([A-Za-z0-9_.]+)|\bproject\s*\(\s*"(:[^"\n]+)"\s*\)')
PLUGIN = re.compile(r'\b(?:id|apply)\s*\(\s*"([^"]+)"\s*\)')
RETIRED = re.compile(r'org\.mobilenativefoundation\.trails\.(?:domain|network|server)(?:\.|$)|(?:^|\.)store5(?:\.|$)')
UI = re.compile(r'\bcompose\.|\bcircuit\.|androidx\.compose|com\.slack\.circuit|org\.jetbrains\.compose|libs\.coil\.compose|libs\.plugins\.(?:compose\w*|circuit\w*)|libs\.androidx\.[\w.]*[Cc]ompose')


def without_comments(text):
    return COMMENTS.sub(lambda match: match[0] if match[0].startswith('"') else re.sub(r'[^\n]', ' ', match[0]), text)


def files_under(root, name=None):
    for directory, children, files in os.walk(root):
        children[:] = sorted(child for child in children if child not in IGNORED)
        for filename in sorted(files):
            if name is None or filename == name:
                yield Path(directory) / filename


def test_ranges(text):
    """Find test source-set blocks, including the compact commonTest.dependencies form."""
    stack, ranges = [], []
    previous = 0
    for match in re.finditer(r'"(?:\\.|[^"\\])*"|[{}]', text):
        if match[0] == '{':
            header = text[max(previous, text.rfind('\n', 0, match.start()) + 1):match.start()]
            stack.append((match.end(), bool(re.search(r'\b(?:\w*Test|test)\b', header))))
            previous = match.end()
        elif match[0] == '}':
            if stack:
                start, is_test = stack.pop()
                if is_test:
                    ranges.append((start, match.start()))
            previous = match.end()
    return ranges


def production_at(text, offset, ranges):
    if any(start <= offset < end for start, end in ranges):
        return False
    statement = text[max(text.rfind('\n', 0, offset), text.rfind(';', 0, offset)) + 1:offset]
    return not re.search(r'\b\w*[Tt]est\w*\s*\(', statement)


def project_dependencies(text):
    text = without_comments(text)
    ranges = test_ranges(text)
    for match in PROJECT.finditer(text):
        path = ':' + match[1].replace('.', ':') if match[1] else match[2]
        yield path, production_at(text, match.start(), ranges), text.count('\n', 0, match.start()) + 1


def convention_sources(root):
    registration = root / 'tooling/plugins/build.gradle.kts'
    if not registration.exists():
        return {}
    text = without_comments(registration.read_text())
    result = {}
    for plugin, implementation in re.findall(r'\bid\s*=\s*"([^"]+)"\s+implementationClass\s*=\s*"([^"]+)"', text):
        result[plugin] = root / 'tooling/plugins/src/main/kotlin' / (implementation.replace('.', '/') + '.kt')
    return result


def find_cycles(graph):
    done, active, cycles = set(), [], []

    def visit(node):
        if node in active:
            cycles.append(active[active.index(node):] + [node])
            return
        if node in done:
            return
        active.append(node)
        for dependency in sorted(graph.get(node, ())):
            visit(dependency)
        active.pop()
        done.add(node)

    for node in sorted(graph):
        visit(node)
    return cycles


def check(root):
    errors = []
    settings = without_comments((root / 'settings.gradle.kts').read_text())
    modules = set()
    for declaration in re.finditer(r'(?m)^\s*include\s*\(([^)]*)\)', settings):
        modules.update(re.findall(r'"(:[^"\n]+)"', declaration[1]))
    if not modules:
        return ['settings.gradle.kts: no literal modules found'], 0
    graph = {module: set() for module in modules}
    conventions = convention_sources(root)

    def violation(path, line, message):
        errors.append(f'{path.relative_to(root)}:{line}: {message}')

    for section in ('apps', 'multiplatform'):
        for build in files_under(root / section, 'build.gradle.kts'):
            module = ':' + ':'.join(build.parent.relative_to(root).parts)
            if module not in modules:
                violation(build, 1, 'build file is not included in root settings; remove it or include the module')

    for module in sorted(modules):
        build = root.joinpath(*module.strip(':').split(':'), 'build.gradle.kts')
        if not build.is_file():
            errors.append(f'settings.gradle.kts: included module {module} has no build.gradle.kts')
            continue
        sources, seen_plugins = [(build, without_comments(build.read_text()))], set()
        index = 0
        while index < len(sources):
            source, text = sources[index]
            index += 1
            for plugin in PLUGIN.findall(text):
                if plugin in seen_plugins:
                    continue
                seen_plugins.add(plugin)
                if plugin.startswith('plugin.trails.') and plugin not in conventions:
                    violation(source, 1, f'{module} applies unregistered convention {plugin}')
                elif plugin in conventions:
                    implementation = conventions[plugin]
                    if implementation.is_file():
                        sources.append((implementation, without_comments(implementation.read_text())))
                    else:
                        violation(source, 1, f'convention {plugin} implementation is missing')
        data_module = module.startswith(':multiplatform:data:')
        if data_module and 'plugin.trails.library' not in PLUGIN.findall(sources[0][1]):
            violation(build, 1, 'data modules must apply plugin.trails.library (plus DI or storage plugins as needed)')
        if data_module and any('compose' in plugin.lower() or 'circuit' in plugin.lower() or plugin == 'plugin.trails.feature' for plugin in seen_plugins):
            violation(build, 1, 'data modules must not apply Compose/Circuit conventions')

        for source, text in sources:
            for dependency, production, line in project_dependencies(text):
                if dependency not in modules:
                    violation(source, line, f'{module} references inactive module {dependency}')
                if not production:
                    continue
                graph[module].add(dependency)
                if dependency.endswith(':impl') and module != ':multiplatform:app:runtime' and not module.startswith(':apps:'):
                    violation(source, line, f'{module} consumes {dependency}; production implementations are composed only by app/runtime or app hosts')
                if data_module and re.match(r':multiplatform:(?:app|screen|feature|ui):|:multiplatform:foundation:designsystem$', dependency):
                    violation(source, line, f'data module {module} depends on UI/application module {dependency}')
            if data_module:
                ranges = test_ranges(text)
                offset = 0
                for number, line in enumerate(text.splitlines(), 1):
                    if UI.search(line) and production_at(text, offset, ranges):
                        violation(source, number, f'data module {module} declares a UI dependency or plugin')
                    offset += len(line) + 1

    for cycle in find_cycles(graph):
        errors.append('settings.gradle.kts: production dependency cycle: ' + ' -> '.join(cycle))

    for section in ('apps', 'multiplatform', 'tooling', 'integration-tests'):
        for source in files_under(root / section):
            if source.name.startswith('LastRun'):
                violation(source, 1, 'source filename uses the retired app name; name it for its Trails declaration')
            if source.suffix != '.kt' or 'src' not in source.parts:
                continue
            parts = source.relative_to(root).parts
            source_index = parts.index('src')
            if len(parts) <= source_index + 3 or parts[source_index + 2] not in ('kotlin', 'java'):
                continue  # SQLDelight schemas and resources have their own source layout.
            source_set = parts[source_index + 1]
            package = '.'.join(parts[source_index + 3:-1])
            text = without_comments(source.read_text())
            declared = re.search(r'(?m)^\s*package\s+([\w.]+)', text)
            if not declared or declared[1] != package:
                violation(source, 1, f'package must match source directory: {package or "<default>"}')
            if 'test' in source_set.lower():
                continue
            for match in re.finditer(r'(?m)^\s*import\s+([\w.*]+)', text):
                imported = match[1]
                line = text.count('\n', 0, match.start()) + 1
                if RETIRED.search(imported):
                    violation(source, line, f'retired production import {imported}')
                if parts[:2] == ('multiplatform', 'data') and UI.search(imported):
                    violation(source, line, f'data source imports UI API {imported}')
    return sorted(set(errors)), len(modules)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors, count = check(args.root.resolve())
    for error in errors:
        print(error, file=sys.stderr)
    print(f'Architecture check: {count} modules, {len(errors)} violations')
    return bool(errors)


if __name__ == '__main__':
    sys.exit(main())
