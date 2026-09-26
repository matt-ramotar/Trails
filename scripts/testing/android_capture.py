#!/usr/bin/env python3
"""Capture or drive a selected Android device using fresh accessibility hierarchies."""

import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import shlex
import subprocess
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET


class Device:
    def __init__(self, adb, serial, port, output):
        self.adb_path = adb
        self.serial = serial
        self.port = port
        self.output = output

    def adb(self, *args, binary=False):
        result = subprocess.run(
            [self.adb_path, '-P', str(self.port), '-s', self.serial, *args],
            capture_output=True, check=True, timeout=20, stdin=subprocess.DEVNULL,
        )
        return result.stdout if binary else result.stdout.decode()

    def record(self, action, **details):
        self.output.mkdir(parents=True, exist_ok=True)
        with (self.output / 'actions.jsonl').open('a') as stream:
            stream.write(json.dumps(dict(time=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                                         serial=self.serial, action=action, **details)) + '\n')

    def hierarchy(self):
        remote = '/sdcard/trails-' + uuid.uuid4().hex + '.xml'
        try:
            result = self.adb('shell', 'uiautomator', 'dump', remote)
            if 'dumped to' not in result:
                raise RuntimeError('UI hierarchy export failed; no previous hierarchy will be used')
            with tempfile.TemporaryDirectory(prefix='trails-hierarchy-') as directory:
                local = Path(directory) / 'tree.xml'
                self.adb('pull', remote, str(local))
                raw = local.read_bytes()
            return raw, ET.fromstring(raw)
        finally:
            try:
                self.adb('shell', 'rm', '-f', remote)
            except subprocess.SubprocessError:
                pass  # Preserve the hierarchy failure if device cleanup also fails.

    def targets(self, name):
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', name):
            raise ValueError('Capture name must be a filename without directory separators')
        targets = {kind: self.output / (name + '.' + kind) for kind in ('png', 'xml')}
        if any(path.exists() for path in targets.values()):
            raise ValueError('Capture output already exists')
        self.output.mkdir(parents=True, exist_ok=True)
        return targets

    def capture(self, name, remote=None):
        targets = self.targets(name)
        if remote is None:
            xml, tree = self.hierarchy()
            png = self.adb('exec-out', 'screencap', '-p', binary=True)
        else:
            if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', remote):
                raise ValueError('Remote name must be a filename without directory separators')
            with tempfile.TemporaryDirectory(prefix='trails-capture-') as directory:
                for kind in targets:
                    self.adb('pull', '/sdcard/' + remote + '.' + kind, str(Path(directory) / kind))
                xml = (Path(directory) / 'xml').read_bytes()
                png = (Path(directory) / 'png').read_bytes()
            tree = ET.fromstring(xml)
        if not png.startswith(b'\x89PNG\r\n\x1a\n'):
            raise ValueError('Capture is not a PNG')
        for kind, content in (('xml', xml), ('png', png)):
            with targets[kind].open('xb') as stream:
                stream.write(content)
        self.record('capture', name=name, remote=remote,
                    pngSha256=hashlib.sha256(png).hexdigest(), xmlSha256=hashlib.sha256(xml).hexdigest())
        print_nodes(tree)


def labelled_nodes(tree):
    return [node for node in tree.iter('node') if node.get('text') or node.get('content-desc')]


def print_nodes(tree):
    for node in labelled_nodes(tree):
        print(f"{node.get('text') or node.get('content-desc')} | {node.get('bounds')} | checked={node.get('checked')}")


def find_node(tree, label, index, slider=False):
    found = [node for node in labelled_nodes(tree) if label in (node.get('text'), node.get('content-desc'))]
    found.sort(key=lambda node: (node.get('content-desc') != label, node.get('clickable') != 'true'))
    if slider:
        parents = {child: parent for parent in tree.iter() for child in parent}
        targets = []
        for node in found:
            while node.get('class') != 'android.widget.SeekBar' and node in parents:
                node = parents[node]
            if node.get('class') == 'android.widget.SeekBar' and node not in targets:
                targets.append(node)
        found = targets
    if index < 0 or index >= len(found):
        raise ValueError(f'No node {label!r} at index {index}')
    return found[index]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=shutil.which('adb') or str(Path(os.environ.get('ANDROID_HOME', '~/Library/Android/sdk')).expanduser() / 'platform-tools/adb'))
    parser.add_argument('--serial', required=True)
    parser.add_argument('--port', type=int, default=5037)
    parser.add_argument('--output-dir', type=Path, required=True)
    parser.add_argument('--index', type=int, default=0)
    parser.add_argument('--fraction', type=float, default=0.5)
    parser.add_argument('--timeout', type=float, default=20)
    parser.add_argument('--remote', help='Basename of an existing /sdcard PNG/XML pair for pull')
    parser.add_argument('action', choices=('capture', 'pull', 'tap', 'slide', 'text', 'key', 'wait', 'nodes'))
    parser.add_argument('value', nargs='?', default='')
    args = parser.parse_args()
    if not 0 <= args.fraction <= 1 or not 0 < args.timeout <= 60:
        parser.error('fraction must be 0..1 and timeout must be greater than 0 and at most 60 seconds')
    if not 1 <= args.port <= 65535:
        parser.error('port must be 1..65535')
    device = Device(args.adb, args.serial, args.port, args.output_dir)
    try:
        if args.action in ('capture', 'pull'):
            device.capture(args.value, (args.remote or args.value) if args.action == 'pull' else None)
        elif args.action == 'nodes':
            print_nodes(device.hierarchy()[1])
        elif args.action in ('tap', 'slide'):
            node = find_node(device.hierarchy()[1], args.value, args.index, args.action == 'slide')
            bounds = list(map(int, re.findall(r'-?\d+', node.get('bounds', ''))))
            if len(bounds) != 4:
                raise ValueError('Node has no usable bounds')
            x, y = (bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2
            if args.action == 'slide':
                target = round(bounds[0] + args.fraction * (bounds[2] - bounds[0] - 1))
                device.adb('shell', 'input', 'touchscreen', 'swipe', str(x), str(y), str(target), str(y), '250')
            else:
                device.adb('shell', 'input', 'tap', str(x), str(y))
            device.record(args.action, label=args.value, index=args.index, fraction=args.fraction, x=x, y=y)
        elif args.action in ('text', 'key'):
            command = 'text' if args.action == 'text' else 'keyevent'
            device.adb('shell', 'input', command, shlex.quote(args.value.replace(' ', '%s')) if command == 'text' else shlex.quote(args.value))
            device.record(args.action, value=args.value)
        elif args.action == 'wait':
            deadline = time.monotonic() + args.timeout
            while True:
                if any(args.value in (node.get('text'), node.get('content-desc')) for node in labelled_nodes(device.hierarchy()[1])):
                    device.record('observed', label=args.value)
                    return
                if time.monotonic() >= deadline:
                    raise TimeoutError(f'Timed out waiting for {args.value!r}')
                time.sleep(0.25)
    except Exception as failure:
        device.record('failure', operation=args.action, error=str(failure))
        raise


if __name__ == '__main__':
    main()
