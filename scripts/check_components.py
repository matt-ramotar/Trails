#!/usr/bin/env python3
"""Check direct Material visual-control usage outside the Trails design system.

This lexical check covers authored production Kotlin under apps and multiplatform.
It does not resolve Kotlin types, validate adapter appearance, or replace the full
component inventory in docs/components.md.
"""

import argparse
import os
from pathlib import Path
import re
import sys


IGNORED = {'build', '.gradle', '.git', '.kotlin', '__pycache__'}
DESIGN_SYSTEM = ('multiplatform', 'foundation', 'designsystem')
ACCOUNT_SHELL = ('multiplatform/app/runtime/src/commonMain/kotlin/org/'
                 'mobilenativefoundation/trails/app/runtime/AccountContent.kt')
CONTROLS = set('''
AlertDialog BasicAlertDialog Badge BadgedBox BottomAppBar BottomDrawer
BottomNavigation BottomNavigationItem BottomSheetScaffold Button Card Checkbox Chip
CircularProgressIndicator DatePicker DatePickerDialog DateRangePicker Divider
DockedSearchBar DropdownMenu DropdownMenuItem ElevatedButton ElevatedCard
ElevatedFilterChip ElevatedSuggestionChip ExposedDropdownMenuBox ExtendedFloatingActionButton
FilledIconButton FilledIconToggleButton FilledTonalButton FilledTonalIconButton
FilledTonalIconToggleButton FilterChip FloatingActionButton HorizontalDivider IconButton
IconToggleButton InputChip AssistChip SuggestionChip LargeFloatingActionButton
LargeTopAppBar LeadingIconTab LinearProgressIndicator ListItem MediumTopAppBar
ModalBottomSheet ModalBottomSheetLayout ModalDrawer ModalDrawerSheet ModalNavigationDrawer
MultiChoiceSegmentedButtonRow NavigationBar NavigationBarItem NavigationDrawerItem
NavigationRail NavigationRailItem OutlinedButton OutlinedCard OutlinedIconButton
OutlinedIconToggleButton OutlinedTextField PermanentDrawerSheet PermanentNavigationDrawer
PlainTooltip PullToRefreshBox RadioButton RangeSlider RichTooltip Scaffold ScrollableTabRow
SearchBar SecondaryScrollableTabRow SecondaryTabRow SegmentedButton ShortNavigationBar
ShortNavigationBarItem SingleChoiceSegmentedButtonRow Slider SmallFloatingActionButton
Snackbar SnackbarHost SuggestionChip Surface SwipeToDismiss SwipeToDismissBox Switch Tab
TabRow TextButton TextField TimeInput TimePicker TooltipBox TopAppBar TwoRowsTopAppBar
VerticalDivider VerticalSlider VerticalRangeSlider PrimaryTabRow PrimaryScrollableTabRow
'''.split())
VISUAL_APIS = CONTROLS | {name + 'Defaults' for name in CONTROLS} | {
    'MenuDefaults', 'ProgressIndicatorDefaults', 'DrawerDefaults', 'SheetDefaults',
}
IDENTIFIER = r'(?:[A-Za-z_]\w*|`[^`\n]+`)'
IMPORT = re.compile(rf'(?m)^[ \t]*import[ \t]+([\w.*]+)(?:[ \t]+as[ \t]+({IDENTIFIER}))?')
TOKEN = re.compile(rf'{IDENTIFIER}|::|->|[{{}}().;:*<>=,]')
MATERIAL = re.compile(r'^androidx\.compose\.material3?(?:\.|$)')


def code_only(source):
    """Mask comments and literal contents without changing offsets or line numbers."""
    chars = list(source)
    index = 0
    while index < len(source):
        start = index
        if source.startswith('//', index):
            end = source.find('\n', index)
            index = len(source) if end < 0 else end
        elif source.startswith('/*', index):
            index += 2
            depth = 1
            while index < len(source) and depth:
                if source.startswith('/*', index):
                    depth += 1
                    index += 2
                elif source.startswith('*/', index):
                    depth -= 1
                    index += 2
                else:
                    index += 1
        elif source.startswith('"""', index):
            end = source.find('"""', index + 3)
            index = len(source) if end < 0 else end + 3
        elif source[index] in ('"', "'"):
            quote = source[index]
            index += 1
            while index < len(source):
                char = source[index]
                index += 1
                if char == '\\':
                    index += 1
                elif char == quote:
                    break
        else:
            index += 1
            continue
        for offset in range(start, min(index, len(chars))):
            if chars[offset] != '\n':
                chars[offset] = ' '
    return ''.join(chars)


def local_bindings(tokens):
    """Bound common declarations so a local Button is not mistaken for a star import."""
    words = [token[0].strip('`') for token in tokens]
    stack, pairs = [], {}
    for index, word in enumerate(words):
        if word in ('{', '('):
            stack.append(index)
        elif word in ('}', ')') and stack:
            opening = stack.pop()
            pairs[opening] = index
    blocks = [(start, end) for start, end in pairs.items() if words[start] == '{']

    def scope(index):
        return max(((start, end) for start, end in blocks if start < index < end),
                   default=(-1, len(words)))

    bindings = []
    for index, word in enumerate(words[:-1]):
        if word in ('val', 'var', 'class', 'object', 'typealias'):
            name = words[index + 1]
            if name in VISUAL_APIS:
                start, end = scope(index)
                bindings.append((name, index if word in ('val', 'var') else start, end))
        elif word == 'fun':
            opening = next((i for i in range(index + 1, len(words)) if words[i] in ('(', '{', '=')), None)
            if opening is None or words[opening] != '(' or opening not in pairs:
                continue
            name = words[opening - 1]
            if name in VISUAL_APIS:
                bindings.append((name, *scope(index)))
            closing = pairs[opening]
            body = next((i for i in range(closing + 1, len(words)) if words[i] in ('{', '=', ';')), None)
            if body is None:
                continue
            end = pairs.get(body, scope(index)[1])
            for parameter in range(opening + 1, closing):
                if words[parameter] in VISUAL_APIS and words[parameter + 1] == ':':
                    bindings.append((words[parameter], opening, end))
    return bindings


def source_violations(source, relative):
    code = code_only(source)
    errors = []
    wildcard = False
    imported_names = set()

    def forbidden(symbol):
        return symbol in VISUAL_APIS and not (symbol == 'Scaffold' and relative == ACCOUNT_SHELL)

    def report(offset, symbol):
        line = source.count('\n', 0, offset) + 1
        errors.append(f'{relative}:{line}: use a Trails adapter instead of Material {symbol}')

    masked = list(code)
    for match in IMPORT.finditer(code):
        qualified, alias = match.groups()
        symbol = qualified.rsplit('.', 1)[-1]
        if MATERIAL.match(qualified):
            visual_owner = next((part for part in qualified.split('.')[3:] if forbidden(part)), None)
            if visual_owner:
                report(match.start(), visual_owner)
            elif symbol == '*':
                wildcard = True
        if symbol != '*':
            imported_names.add((alias or symbol).strip('`'))
        for index in range(match.start(), match.end()):
            if masked[index] != '\n':
                masked[index] = ' '
    tokens = [(match[0], match.start()) for match in TOKEN.finditer(''.join(masked))]
    words = [token[0].strip('`') for token in tokens]
    bindings = local_bindings(tokens)
    for index, (word, offset) in enumerate(tokens):
        word = word.strip('`')
        if word == 'androidx':
            chain = [word]
            cursor = index + 1
            while cursor + 1 < len(words) and words[cursor] == '.':
                chain.append(words[cursor + 1])
                cursor += 2
            if chain[:2] == ['androidx', 'compose'] and len(chain) > 3 and chain[2] in ('material', 'material3'):
                symbol = next((part for part in chain[3:] if forbidden(part)), None)
                if symbol:
                    report(offset, symbol)
        if not wildcard or not forbidden(word) or word in imported_names:
            continue
        previous = words[index - 1] if index else None
        if previous == '.' or any(name == word and start < index < end for name, start, end in bindings):
            continue
        following = words[index + 1] if index + 1 < len(words) else None
        if following in ('(', '{', '.') or previous == '::':
            report(offset, word)
    return sorted(set(errors))


def production_sources(root):
    for section in ('apps', 'multiplatform'):
        for directory, children, names in os.walk(root / section):
            children[:] = sorted(name for name in children if name not in IGNORED)
            for name in sorted(names):
                path = Path(directory) / name
                parts = path.relative_to(root).parts
                if path.suffix != '.kt' or parts[:3] == DESIGN_SYSTEM or 'src' not in parts:
                    continue
                source_index = parts.index('src')
                if len(parts) <= source_index + 3 or parts[source_index + 2] not in ('kotlin', 'java'):
                    continue
                if 'test' not in parts[source_index + 1].lower():
                    yield path


def check(root):
    errors, count = [], 0
    for path in production_sources(root):
        count += 1
        errors.extend(source_violations(path.read_text(), path.relative_to(root).as_posix()))
    return sorted(set(errors)), count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors, count = check(args.root.resolve())
    for error in errors:
        print(error, file=sys.stderr)
    print(f'Component check: {count} sources, {len(errors)} violations')
    return bool(errors)


if __name__ == '__main__':
    sys.exit(main())
