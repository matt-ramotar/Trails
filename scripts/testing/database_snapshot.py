#!/usr/bin/env python3
"""Summarize a copied Trails Android databases tar without device access or raw payload output.

Usage: python3 scripts/testing/database_snapshot.py snapshot.tar --output evidence.json \
           --capture-state force-stopped

Includes copied WAL files (never immutable=1), opens databases read-only, and extracts only
allowlisted Trails files into a disposable directory. This is a schema-pinned evidence reader,
not a Store6 runtime API. Raw accounts, command IDs, query text, messages, and blobs are omitted.
"""
from __future__ import annotations

import argparse
import datetime
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
import hashlib
import json
import math
from pathlib import Path, PurePosixPath
import re
import sqlite3
import sys
import tarfile
import tempfile

SCRIPT_VERSION = 2
STORE6_REVISION = '582edfe86e64ddc71312ecd20a1895fc3de37b52'
REPOSITORY = Path(__file__).resolve().parents[2]
PHOTO_MANIFEST = REPOSITORY / 'multiplatform/ui/trail/src/commonMain/composeResources/files/trail_photography.json'
TRAILS = frozenset(photo['trailId'] for photo in json.loads(PHOTO_MANIFEST.read_text())['photos'])
COLLECTIONS = frozenset(('weekend', 'favorites'))
PHASES = frozenset(('UNPREPARED', 'READY', 'INFLIGHT', 'REFRESH_REQUIRED', 'ACKED',
                    'EFFECTS_PENDING', 'PARKED', 'RETIRED'))
FAILURES = frozenset(('IDENTITY', 'CODEC', 'PROJECTION', 'PROTOCOL', 'CONFLICT',
                      'TRANSPORT', 'ADOPTION', 'EFFECT', 'PERSISTENCE'))
DB_NAME = re.compile(r'^trails-m1-(?:(backend|catalog)|([0-9a-f]+)-(values|journal))\.db$')
DEFAULT_CONFIG = dict(version=1, mode='ONLINE', minMs=50, maxMs=200, errorRate=0,
                      rateLimit=0, conflictMode='DISABLED', conflictProbability=0,
                      seed='RANDOM', pageSize=20, maxPageSize=100)
ENUM_CONFIG = dict(mode={'ONLINE', 'OFFLINE'}, conflictMode={'DISABLED', 'HTTP_409', 'LAST_WRITE_WINS', 'AUTO_MERGE'},
                   seed={'RANDOM', 'SEED_42', 'SEED_1337', 'SEED_9001'})


def digest(value):
    if value is None:
        return None
    if not isinstance(value, bytes):
        value = str(value).encode('utf-8')
    return hashlib.sha256(value).hexdigest()


def file_digest(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        while chunk := stream.read(1024 * 1024):
            result.update(chunk)
    return result.hexdigest()


def page_ids(value):
    parsed = parse_json(value)
    trails = parsed.get('trails') if isinstance(parsed, dict) else None
    if not isinstance(trails, list):
        return None
    return [safe_id(trail.get('id'), TRAILS) for trail in trails if isinstance(trail, dict)]


def safe_id(value, known=frozenset()):
    return value if isinstance(value, str) and value in known else {'sha256': digest(value)}


def parse_json(value):
    try:
        return json.loads(value)
    except (ValueError, TypeError, UnicodeError):
        return None


def membership(value):
    """Summarize the two membership fields from a versioned SavedValue blob."""
    parsed = parse_json(value)
    result = {'payloadSha256': digest(value)}
    if isinstance(parsed, dict) and isinstance(parsed.get('trailId'), str) and isinstance(parsed.get('collectionIds'), list):
        result.update(trail=safe_id(parsed['trailId'], TRAILS),
                      collections=[safe_id(item, COLLECTIONS) for item in sorted(parsed['collectionIds'], key=str)],
                      recognized=True)
    else:
        result['recognized'] = False
    return result


def command(value):
    parsed = parse_json(value)
    result = {'payloadSha256': digest(value), 'recognized': False}
    if isinstance(parsed, dict) and isinstance(parsed.get('value'), dict):
        desired = membership(json.dumps(parsed['value'], separators=(',', ':')))
        desired.pop('payloadSha256')  # This nested value was decoded, not independently stored bytes.
        result.update(commandSha256=digest(parsed.get('commandId')), accountSha256=digest(parsed.get('accountId')),
                      desired=desired, recognized=desired['recognized'])
    return result


def configuration(value):
    parsed = parse_json(value)
    if not isinstance(parsed, dict) or parsed.get('version', 1) != 1:
        return {'recognized': False, 'payloadSha256': digest(value)}
    configured = DEFAULT_CONFIG | {key: parsed[key] for key in DEFAULT_CONFIG if key in parsed}
    for key, item in configured.items():
        if key in ENUM_CONFIG:
            configured[key] = safe_id(item, ENUM_CONFIG[key])
        elif not isinstance(item, (int, float)) or isinstance(item, bool) or not math.isfinite(item):
            configured[key] = {'sha256': digest(item)}
    return {'recognized': True, 'effective': configured, 'payloadSha256': digest(value),
            'defaultedFields': sorted(set(DEFAULT_CONFIG) - set(parsed))}


def db_identity(name):
    match = DB_NAME.fullmatch(name)
    if not match:
        return None
    public, encoded, role = match.groups()
    result = {'role': public or role}
    if encoded:
        try:
            result['accountSha256'] = digest(bytes.fromhex(encoded).decode('utf-8'))
        except (ValueError, UnicodeError):
            raise ValueError('invalid account partition encoding') from None
    return result


def summarize_checkpoint(name, raw):
    encoded = name.removeprefix('trails-m1-ui-').removesuffix('.xml')
    account = bytes.fromhex(encoded).decode('utf-8')
    element = ET.fromstring(raw).find("string[@name='checkpoint']")
    checkpoint = parse_json(element.text) if element is not None else None
    if not isinstance(checkpoint, dict) or not isinstance(checkpoint.get('scroll', {}), dict):
        raise ValueError('Unrecognized view checkpoint payload')
    routes = {}
    for key in ('exploreRoutes', 'savedRoutes', 'forYouRoutes', 'navigateRoutes', 'activityRoutes'):
        if not isinstance(checkpoint.get(key, []), list) or any(not isinstance(route, dict) for route in checkpoint.get(key, [])):
            raise ValueError('Unrecognized view checkpoint routes')
        routes[key] = [dict(name=safe_id(route.get('name'), {'explore', 'saved', 'foryou', 'navigate', 'activity', 'trail', 'collection'}),
                            id=safe_id(route.get('id'), TRAILS | COLLECTIONS))
                       for route in checkpoint.get(key, [])]
    scroll = {digest(key): {'index': value['index'], 'offset': value['offset']}
              for key, value in checkpoint.get('scroll', {}).items()
              if isinstance(value, dict) and type(value.get('index')) is int and type(value.get('offset')) is int}
    return dict(accountSha256=digest(account), preferencesSha256=digest(raw),
                checkpointSha256=digest(element.text if element is not None else None),
                root=safe_id(checkpoint.get('root'), {'EXPLORE', 'FOR_YOU', 'NAVIGATE', 'SAVED', 'ACTIVITY'}),
                routes=routes, scroll=scroll, allTrails=checkpoint.get('allTrails') is True,
                lastTrail=safe_id(checkpoint.get('lastTrail'), TRAILS), querySha256=digest(json.dumps(checkpoint.get('query'), sort_keys=True)),
                searchSha256=digest(checkpoint.get('text')))


def numeric_field(item, name):
    value = item.get(name)
    if type(value) not in (int, float) or not math.isfinite(value):
        raise ValueError('Activity metrics must be finite numbers')
    return value


def summarize_feed(row, as_of, timezone):
    payload = parse_json(row['payload'])
    result = dict(namespace=row['namespace'], accountSha256=digest(row['canonical_id']),
                  payloadSha256=digest(row['payload']))
    if row['namespace'] in ('activities', 'backend-activities') and isinstance(payload, list):
        if any(not isinstance(item, dict) for item in payload):
            raise ValueError('Unrecognized activity payload')
        result['activities'] = [dict(trail=safe_id(item.get('trailId'), TRAILS),
            completedAtEpochMillis=numeric_field(item, 'completedAtEpochMillis'), distanceMeters=numeric_field(item, 'distanceMeters'),
            durationMinutes=numeric_field(item, 'durationMinutes'), elevationMeters=numeric_field(item, 'elevationMeters')) for item in payload]
        if as_of is not None:
            zone = ZoneInfo(timezone)
            month = as_of.strftime('%Y-%m')
            selected = [item for item in result['activities'] if datetime.datetime.fromtimestamp(
                item['completedAtEpochMillis'] / 1000, zone).strftime('%Y-%m') == month]
            minutes = sum(item['durationMinutes'] for item in selected)
            result['monthSummary'] = dict(month=month, timezone=timezone,
                distanceMeters=sum(item['distanceMeters'] for item in selected), trails=len(selected),
                durationMinutes=minutes, outsideWholeHours=minutes // 60)
    elif row['namespace'] in ('foryou', 'backend-foryou') and isinstance(payload, dict):
        if not isinstance(payload.get('recommendedTrailIds', []), list):
            raise ValueError('Unrecognized recommendation payload')
        result['featuredTrail'] = safe_id(payload.get('featuredTrailId'), TRAILS)
        result['anchorTrail'] = safe_id(payload.get('anchorTrailId'), TRAILS)
        result['recommendations'] = [safe_id(value, TRAILS) for value in payload.get('recommendedTrailIds', [])]
    else:
        raise ValueError('Unrecognized account feed payload')
    return result


def extract_databases(archive, directory, max_bytes):
    extracted = {}
    ignored = 0
    checkpoints = []
    seen = set()
    total = 0
    with tarfile.open(archive, 'r|*') as bundle:
        for index, entry in enumerate(bundle):
            if index >= 512:
                raise ValueError('archive has too many entries')
            path = PurePosixPath(entry.name)
            if path.is_absolute() or '..' in path.parts or entry.issym() or entry.islnk():
                raise ValueError('archive contains an unsafe path or link')
            if entry.isdir():
                continue
            if not entry.isfile():
                raise ValueError('archive contains a non-regular file')
            total += entry.size
            if entry.size < 0 or total > max_bytes:
                raise ValueError('archive exceeds the uncompressed size bound')
            name = path.name
            if str(path) in seen:
                raise ValueError('archive contains a duplicate member')
            seen.add(str(path))
            if path.parts[:-1] == ('shared_prefs',) and name.startswith('trails-m1-ui-') and name.endswith('.xml'):
                stream = bundle.extractfile(entry)
                with stream:
                    raw = stream.read()
                checkpoints.append(summarize_checkpoint(name, raw))
                continue
            base = re.sub(r'-(wal|shm|journal)$', '', name)
            if path.parts[:-1] != ('databases',) or not db_identity(base):
                ignored += 1
                continue
            if name in extracted:
                raise ValueError('archive contains a duplicate database member')
            stream = bundle.extractfile(entry)
            if stream is None:
                raise ValueError('archive database member is unreadable')
            target = directory / name
            sha = hashlib.sha256()
            with stream, target.open('xb') as output:
                while chunk := stream.read(1024 * 1024):
                    output.write(chunk)
                    sha.update(chunk)
            extracted[name] = {'sha256': sha.hexdigest(), 'bytes': target.stat().st_size}
    return extracted, ignored, checkpoints


class Database:
    def __init__(self, path, row_limit):
        self.connection = sqlite3.connect(path.as_uri() + '?mode=ro', uri=True)
        self.connection.row_factory = sqlite3.Row
        self.connection.execute('PRAGMA query_only = ON')
        self.connection.execute('PRAGMA trusted_schema = OFF')
        self.tables = {row[0] for row in self.connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        self.limit = row_limit
        self.truncated = False

    def rows(self, sql, args=()):
        rows = self.connection.execute(sql + ' LIMIT ?', (*args, self.limit + 1)).fetchall()
        self.truncated |= len(rows) > self.limit
        return rows[:self.limit]

    def count(self, table):
        # All callers pass static allowlisted table names, never archive-provided SQL.
        return self.connection.execute('SELECT COUNT(*) FROM ' + table).fetchone()[0]

    def version(self, table):
        if table not in self.tables:
            return None
        return self.connection.execute('SELECT version FROM ' + table + ' WHERE id=0').fetchone()[0]


def summarize(path, identity, row_limit, as_of=None, timezone=None):
    db = Database(path, row_limit)
    try:
        checks = db.connection.execute('PRAGMA integrity_check').fetchall()
        result = dict(identity, integrityOk=len(checks) == 1 and checks[0][0] == 'ok',
                      userVersion=db.connection.execute('PRAGMA user_version').fetchone()[0],
                      store6MetaVersion=db.version('store6_meta_schema'),
                      store6MutationVersion=db.version('store6_mutation_schema'))
        required = {'cache_row', 'command_acceptance', 'backend_meta', 'backend_receipt', 'account_identity'}
        if not required <= db.tables:
            result.update(complete=False, error='Trails schema is missing required tables')
            return result
        result['debugFaults'] = {key: db.connection.execute(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name=?", (key,)).fetchone()[0] > 0
            for key in ('trails_m1_debug_fail_admission', 'trails_m1_debug_fail_adoption')}
        role = identity['role']
        if role == 'backend':
            row = db.connection.execute('SELECT * FROM backend_meta WHERE id=1').fetchone()
            result['server'] = None if row is None else {
                'settings': configuration(row['config']), 'schemaVersion': row['version'], 'seedVersion': row['seeded'],
                'applications': row['applications'], 'pushAttempts': row['pushes'], 'requests': row['requests'],
                'loseNextAck': bool(row['lose_ack']), 'receiptCount': db.count('backend_receipt')}
            result['canonicalMemberships'] = [dict(accountSha256=digest(row['account']), value=membership(row['payload']))
                for row in db.rows('SELECT account,payload FROM backend_saved ORDER BY account,trail_id')]
            result['receipts'] = [dict(accountSha256=digest(row['account']), installationSha256=digest(row['installation']),
                idempotencyKeySha256=digest(row['idempotency_key']), trail=safe_id(row['trail_id'], TRAILS), version=row['version'],
                request=membership(row['request']), result=membership(row['result']))
                for row in db.rows('SELECT * FROM backend_receipt ORDER BY account,installation,idempotency_key')]
        elif role == 'catalog':
            result['cacheCounts'] = {row[0] if row[0] in ('trail', 'query') else digest(row[0]): row[1]
                for row in db.rows('SELECT namespace,COUNT(*) FROM cache_row GROUP BY namespace ORDER BY namespace')}
            result['cachedQueries'] = [dict(querySha256=digest(row['canonical_id']), payloadSha256=digest(row['payload']),
                trailIds=page_ids(row['payload']))
                for row in db.rows("SELECT canonical_id,payload FROM cache_row WHERE namespace='query' ORDER BY canonical_id")]
        elif role == 'values':
            result['confirmedMemberships'] = [dict(namespaceSha256=digest(row['namespace']), trail=safe_id(row['canonical_id'], TRAILS),
                value=membership(row['payload'])) for row in db.rows("SELECT * FROM cache_row WHERE namespace LIKE 'saved-%' ORDER BY namespace,canonical_id")]
            result['collections'] = [safe_id(row[0], COLLECTIONS) for row in db.rows('SELECT id FROM collection_row ORDER BY position')]
        elif role == 'journal':
            row = db.connection.execute('SELECT account,installation_id FROM account_identity WHERE id=1').fetchone()
            result['identity'] = None if row is None else dict(accountSha256=digest(row['account']), installationSha256=digest(row['installation_id']))
            result['identityMatchesPartition'] = row is not None and digest(row['account']) == identity['accountSha256']
            result['acceptanceCount'] = db.count('command_acceptance')
            result['acceptances'] = [dict(accountSha256=digest(row['account']), commandSha256=digest(row['command_id']),
                clientSha256=digest(row['client_id']), clientSequence=row['client_sequence'], mutationSha256=digest(row['mutation_id']),
                version=row['version'], admittedAt=row['admitted_at'], command=command(row['payload']))
                for row in db.rows('SELECT * FROM command_acceptance ORDER BY client_id,client_sequence')]
            if 'store6_mutation_execution' in db.tables:
                result['phaseCounts'] = {phase if phase in PHASES else digest(phase): count for phase, count in
                    db.rows('SELECT phase,COUNT(*) FROM store6_mutation_execution GROUP BY phase ORDER BY phase')}
                result['executions'] = [dict(clientSha256=digest(row['client_id']), clientSequence=row['client_sequence'],
                    phase=safe_id(row['phase'], PHASES), generation=row['current_generation'], attempt=row['attempt'],
                    lastAttemptAt=row['last_attempt_at'], activeFailureId=row['active_failure_id'], retiredAt=row['retired_at'])
                    for row in db.rows('SELECT * FROM store6_mutation_execution ORDER BY client_id,client_sequence')]
                result['intents'] = [dict(clientSha256=digest(row['client_id']), clientSequence=row['client_sequence'],
                    mutationSha256=digest(row['mutation_id']), namespaceSha256=digest(row['namespace']), trail=safe_id(row['canonical_id'], TRAILS),
                    argumentVersion=row['mutator_version'], command=command(row['args_blob']))
                    for row in db.rows('SELECT * FROM store6_mutation_intent ORDER BY client_id,client_sequence')]
                result['attempts'] = [dict(clientSha256=digest(row['client_id']), clientSequence=row['client_sequence'],
                    generation=row['generation'], idempotencyKeySha256=digest(row['generation_idempotency_key']),
                    base=membership(row['base_blob']), desired=membership(row['mine_blob']))
                    for row in db.rows('SELECT * FROM store6_mutation_attempt ORDER BY client_id,client_sequence,generation')]
                result['acks'] = [dict(clientSha256=digest(row['client_id']), clientSequence=row['client_sequence'],
                    generation=row['generation'], receivedAt=row['received_at'], authoritative=membership(row['authoritative_blob']))
                    for row in db.rows('SELECT * FROM store6_mutation_ack ORDER BY client_id,client_sequence,generation')]
                result['failures'] = [dict(id=row['failure_id'], clientSequence=row['client_sequence'], generation=row['generation'],
                    kind=safe_id(row['kind'], FAILURES), detailSha256=digest(row['detail']), messageSha256=digest(row['message']), occurredAt=row['occurred_at'])
                    for row in db.rows('SELECT * FROM store6_mutation_failure ORDER BY failure_id')]
                result['clients'] = [dict(clientSha256=digest(row['client_id']), lastAllocatedSequence=row['last_allocated_sequence'],
                    retiredThroughSequence=row['retired_through_sequence'], serverConfirmedRetiredThroughSequence=row['server_confirmed_retired_through_sequence'])
                    for row in db.rows('SELECT * FROM store6_mutation_client ORDER BY client_id')]
                result['retainedRows'] = {table: db.count(table) for table in (
                    'store6_mutation_intent', 'store6_mutation_execution', 'store6_mutation_attempt',
                    'store6_mutation_ack', 'store6_mutation_failure', 'store6_mutation_effect')}
            else:
                result['journalInitialized'] = False
        if role == 'backend' and 'backend_trail' in db.tables:
            catalog = []
            for row in db.rows('SELECT id,payload,position FROM backend_trail ORDER BY position'):
                trail = parse_json(row['payload'])
                if not isinstance(trail, dict) or not isinstance(trail.get('activities', []), list):
                    raise ValueError('Unrecognized catalog payload')
                catalog.append(dict(trail=safe_id(row['id'], TRAILS), position=row['position'],
                    difficulty=safe_id(trail.get('difficulty'), {'EASY', 'MODERATE', 'HARD', 'STRENUOUS'}),
                    activities=[safe_id(activity, {'HIKING', 'BACKPACKING'}) for activity in trail.get('activities', [])]))
            result['catalog'] = catalog
        if role in ('backend', 'values'):
            result['feeds'] = [summarize_feed(row, as_of, timezone) for row in db.rows(
                "SELECT namespace,canonical_id,payload FROM cache_row WHERE namespace IN ('backend-activities','backend-foryou','activities','foryou') ORDER BY namespace,canonical_id")]
        result['truncated'] = db.truncated
        result['complete'] = (result['integrityOk'] and not db.truncated
                              and result.get('journalInitialized', True)
                              and (role != 'backend' or result.get('server') is not None)
                              and result.get('identityMatchesPartition', True)
                              and result['store6MutationVersion'] in (None, 2)
                              and result['store6MetaVersion'] in (None, 1))
        return result
    finally:
        db.connection.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('archive', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--capture-state', choices=('unknown', 'idle', 'force-stopped'), default='unknown',
                        help='Caller-declared capture state; this reader cannot verify device state')
    parser.add_argument('--as-of', type=datetime.date.fromisoformat, help='Date selecting the activity summary month')
    parser.add_argument('--timezone', help='IANA time zone for activity summary dates')
    parser.add_argument('--max-mib', type=int, default=256)
    parser.add_argument('--row-limit', type=int, default=1000)
    args = parser.parse_args()
    if bool(args.as_of) != bool(args.timezone):
        parser.error('--as-of and --timezone must be supplied together')
    if args.timezone:
        try:
            ZoneInfo(args.timezone)
        except ZoneInfoNotFoundError:
            parser.error('unknown IANA time zone')
    if args.max_mib <= 0 or args.row_limit <= 0:
        parser.error('size and row bounds must be positive')
    archive = args.archive.resolve(strict=True)
    output = args.output.resolve()
    if archive == output or output.exists():
        parser.error('output must be a new path, separate from the archive; evidence is never overwritten')
    report = {'readerVersion': SCRIPT_VERSION, 'store6SourceRevision': STORE6_REVISION,
              'archiveSha256': file_digest(archive),
              'captureStateDeclaredByCaller': args.capture_state,
              'limits': {'maxMiB': args.max_mib, 'rowLimit': args.row_limit},
              'privacy': 'Raw account, command, installation, query and failure text and arbitrary payload fields are omitted; SHA256 identifiers allow comparisons.',
              'meaningOfComplete': 'Extraction and supported schema reading completed without truncation; this is not an application acceptance result.',
              'consistency': 'Per-file integrity is checked. A tar copied from a running app is not an atomic multi-database snapshot.',
              'databases': [], 'checkpoints': []}
    try:
        with tempfile.TemporaryDirectory(prefix='trails-database-snapshot-') as temporary:
            directory = Path(temporary)
            members, ignored, checkpoints = extract_databases(archive, directory, args.max_mib * 1024 * 1024)
            report['ignoredFiles'] = ignored
            report['checkpoints'] = checkpoints
            for name in sorted(members):
                identity = db_identity(name)
                if identity is None:
                    continue
                files = {suffix or 'database': members[name + suffix] for suffix in ('', '-wal', '-shm', '-journal') if name + suffix in members}
                try:
                    summary = summarize(directory / name, identity, args.row_limit, args.as_of, args.timezone)
                except (sqlite3.Error, ValueError, TypeError, KeyError, OverflowError) as error:
                    summary = dict(identity, complete=False, errorType=type(error).__name__,
                                   sqliteError=getattr(error, 'sqlite_errorname', None))
                report['databases'].append(dict(summary, archivedFiles=files))
            report['complete'] = bool(report['databases']) and all(item['complete'] for item in report['databases'])
    except (tarfile.TarError, ValueError, OSError, ET.ParseError) as error:
        report.update(complete=False, archiveErrorType=type(error).__name__)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open('x') as target:
        json.dump(report, target, indent=2, sort_keys=True, allow_nan=False)
        target.write('\n')
    print(json.dumps({'complete': report['complete'], 'databases': len(report['databases']), 'output': str(output)}))
    return 0 if report['complete'] else 2


if __name__ == '__main__':
    sys.exit(main())
