#!/usr/bin/env python3
"""Bounded M1 emulator evidence helper. Never clears app data or changes backend settings."""
import argparse
import os
import datetime
import hashlib
import json
from pathlib import Path
import subprocess
import time
import tempfile
import uuid
import xml.etree.ElementTree as ET
import re

ROOT = Path(__file__).resolve().parents[4]  # repository root
ADB = Path.home() / 'Library/Android/sdk/platform-tools/adb'
EVIDENCE = ROOT / 'docs/evidence/roots/android'
EVIDENCE.mkdir(parents=True, exist_ok=True)


def adb(*args, binary=False):
    args = ('shell', '-T', '-n', *args[1:]) if args and args[0] == 'shell' else args
    result = subprocess.run([str(ADB), '-P', os.environ.get('ROOTS_ADB_PORT', '5037'), '-s', 'emulator-5554', *args], capture_output=True, check=True, timeout=20, stdin=subprocess.DEVNULL)
    return result.stdout if binary else result.stdout.decode()


def record(action, **details):
    with (EVIDENCE / 'actions.jsonl').open('a') as output:
        output.write(json.dumps(dict(time=datetime.datetime.now(datetime.timezone.utc).isoformat(), action=action, **details)) + '\n')


def hierarchy():
    remote = '/sdcard/trails-roots-' + uuid.uuid4().hex + '.xml'
    exported = adb('shell', 'uiautomator', 'dump', remote)
    if 'dumped to' not in exported:
        record('hierarchy-export-failure', exporter=exported)
        raise RuntimeError('UI hierarchy export failed; no stale hierarchy may be used')
    with tempfile.TemporaryDirectory(prefix='roots-tree-') as tmp:
        local = Path(tmp) / 'tree.xml'
        adb('pull', remote, str(local))
        xml = local.read_text()
    if not xml.lstrip().startswith('<?xml'):
        record('hierarchy-export-failure', exporter=exported, raw=xml)
        raise RuntimeError('UI hierarchy unavailable; inspect launch readiness before continuing')
    return xml, ET.fromstring(xml)


def nodes(tree):
    return [n for n in tree.iter('node') if n.get('text') or n.get('content-desc')]


def capture(name):
    # Export first so post-input transition frames are not mislabeled as the stable UI.
    try:
        xml, tree = hierarchy()
    except Exception:
        shot = adb('exec-out', 'screencap', '-p', binary=True)
        (EVIDENCE / (name + '-export-failure.png')).write_bytes(shot)
        raise
    shot = adb('exec-out', 'screencap', '-p', binary=True)
    target = EVIDENCE / (name + '.png')
    target.write_bytes(shot)
    # Stored at 844 px tall so the committed evidence stays small; the raw digest is recorded too.
    subprocess.run(['sips', '-Z', '844', str(target)], capture_output=True, check=True, timeout=20, stdin=subprocess.DEVNULL)
    (EVIDENCE / (name + '.xml')).write_text(xml)
    record('capture', name=name, capturedPngSha256=hashlib.sha256(shot).hexdigest(),
           storedPngSha256=hashlib.sha256(target.read_bytes()).hexdigest(), storedHeightPx=844)
    print('\n'.join(f"{n.get('text') or n.get('content-desc')} | {n.get('bounds')} | checked={n.get('checked')}" for n in nodes(tree)))


p = argparse.ArgumentParser(description=__doc__)
p.add_argument('action', choices=['capture', 'tap', 'slide', 'text', 'key', 'wait', 'nodes'])
p.add_argument('value', nargs='?', default='')
p.add_argument('--index', type=int, default=0)
p.add_argument('--fraction', type=float, default=0.5)
a = p.parse_args()
try:
    if a.action == 'capture': capture(a.value)
    elif a.action == 'nodes':
        _, tree = hierarchy()
        print('\n'.join(f"{n.get('text') or n.get('content-desc')} | {n.get('bounds')}" for n in nodes(tree)))
    elif a.action in ('tap', 'slide'):
        _, tree = hierarchy()
        found = [n for n in nodes(tree) if a.value in (n.get('text'), n.get('content-desc'))]
        found.sort(key=lambda node: (node.get('content-desc') != a.value, node.get('clickable') != 'true'))
        if a.action == 'slide':
            parents = {child: parent for parent in tree.iter() for child in parent}
            sliders = []
            for node in found:
                while node.get('class') != 'android.widget.SeekBar' and node in parents:
                    node = parents[node]
                if node.get('class') == 'android.widget.SeekBar' and node not in sliders:
                    sliders.append(node)
            found = sliders
            if not 0 <= a.fraction <= 1: raise ValueError('fraction must be between zero and one')
        if len(found) <= a.index: raise RuntimeError(f'No node {a.value!r} at index {a.index}')
        bounds = list(map(int, re.findall(r'\d+', found[a.index].get('bounds'))))
        x, y = (bounds[0]+bounds[2])//2, (bounds[1]+bounds[3])//2
        if a.action == 'slide':
            target = round(bounds[0] + a.fraction * (bounds[2] - bounds[0] - 1))
            adb('shell', 'input', 'touchscreen', 'swipe', str(x), str(y), str(target), str(y), '250')
            record('slide', label=a.value, fraction=a.fraction, x=target, y=y)
        else:
            adb('shell', 'input', 'touchscreen', 'tap', str(x), str(y))
            record('tap', label=a.value, index=a.index, x=x, y=y)
    elif a.action == 'text':
        # Android input uses %s for spaces. This is a local sample search, never credentials.
        adb('shell', 'input', 'text', a.value.replace(' ', '%s'))
        record('text', value=a.value)
    elif a.action == 'key':
        adb('shell', 'input', 'keyevent', a.value)
        record('key', value=a.value)
    elif a.action == 'wait':
        deadline = time.monotonic()+20
        while True:
            try:
                _, tree = hierarchy()
            except RuntimeError as failure:
                if 'UI hierarchy export failed' not in str(failure) or time.monotonic() >= deadline:
                    raise
                time.sleep(0.25)
                continue
            if any(a.value in (n.get('text'), n.get('content-desc')) for n in nodes(tree)):
                record('observed', label=a.value)
                print(a.value)
                break
            if time.monotonic() >= deadline: raise RuntimeError(f'Timed out waiting for {a.value!r}')
            time.sleep(0.25)
except Exception as error:
    record('failure', operation=a.action, value=a.value, error=str(error))
    raise
